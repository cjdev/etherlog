package org.teamstory.datas

import java.io.{File => Path}
import scala.collection.mutable.ListBuffer
import org.teamstory.api._
import org.teamstory.api.StatsLogEntry
import scala.Some
import org.teamstory.api.BacklogListEntry
import org.teamstory.api.GlobalConfig
import org.teamstory.api.TeamDto
import org.teamstory.api.HistoryItem
import org.teamstory.api.Item

class DataImpl(val dataPath: Path) extends Data {

  val errors: DatabaseTrait[String] = new DatabaseImpl[String](new Path(dataPath, "errors"))
  val backlogs: DatabaseTrait[BacklogStatus] = new DatabaseImpl[BacklogStatus](new Path(dataPath, "backlogs"))
  val versions: DatabaseTrait[BacklogVersion] = new DatabaseImpl[BacklogVersion](new Path(dataPath, "versions"))
  val config: DatabaseTrait[GlobalConfig] = new DatabaseImpl[GlobalConfig](new Path(dataPath, "config"))
  val teams: DatabaseTrait[TeamDto] = new DatabaseImpl[TeamDto](new Path(dataPath, "teams"))

  def setGlobalConfig(n: GlobalConfig) = {
    config.put("global", n)
  }

  def getGlobalConfig() = {
    if (config.contains("global")) {
      config.get("global")
    } else {
      GlobalConfig()
    }
  }

  val versionsCache = scala.collection.mutable.Map[String, BacklogVersion]()

  override def scanBacklogHistory(backlogId: String, fn: (BacklogVersion) => Unit): Unit = {
    val backlog = backlogs.get(backlogId);
    var nextVersionId = backlog.latestVersion
    while (nextVersionId != null) {
      val version = versionsCache.get(nextVersionId) match {
        case Some(version) => version
        case None => versions.get(nextVersionId)
      }
      versionsCache.put(nextVersionId, version)
      fn(version)
      nextVersionId = version.previousVersion
    }
  }

  override def getHistory(id: String): Seq[HistoryItem] = {
    val results = new ListBuffer[HistoryItem]()
    scanBacklogHistory(id, {
      version =>
        results += HistoryItem(version = version.id, when = version.when, memo = version.backlog.memo)
    })
    results.toSeq
  }

  override def toBacklogListEntry(backlogStatus: BacklogStatus) = {
    val version = versions.get(backlogStatus.latestVersion)
    
    val maybePivotalTrackerId = backlogStatus.pivotalTrackerLink  match {
      case None=>None
      case Some(ptLink)=>Some(ptLink.projectId)
    }
    
    BacklogListEntry(
        id = backlogStatus.id, 
        name = version.backlog.name, 
        whenArchived = backlogStatus.whenArchived,
        pivotalTrackerId=maybePivotalTrackerId)
  }

  override def buildStatsLog(id: String, until: Long, includeCurrentState: Boolean = false): Seq[(StatsLogEntry, BacklogVersion)] = {
    val backlog = backlogs.get(id)
    val allResults = new ListBuffer[(StatsLogEntry, BacklogVersion)]()

    def incrementedEstimate(tally: Int, item: Item) = {
      item.bestEstimate match {
        case Some(value) => {
          value + tally
        }
        case None => tally
      }
    }

    val latestEstimateCurrency = { item: Item =>
       item.estimates match {
         case Some(x: Seq[Estimate]) => {
           def latestWithTime(ee : Seq[Estimate]): (String, Long) = ee match {
             case Nil => ("nada", 0L)
             case e :: er => {
               val found = latestWithTime(er)
               val (_, when) = found
               if(e.when > when) {
                 (e.currency, e.when)
               } else {
                 found
               }
             }
           }
           
           latestWithTime(x)._1
         }
         case None => {
           "nada"
         }
       }
      
    }
    
    scanBacklogHistory(id, {
      version =>
        val items = version.backlog.items
        val amountComplete: Int = items.filter(_.isComplete.getOrElse(false)).foldLeft(0)(incrementedEstimate);
        val amountTeam: Int = items.filter({
          item =>
            !item.isComplete.getOrElse(false) && "team".equals(latestEstimateCurrency(item))
        }
        ).foldLeft(0)(incrementedEstimate);

        val amountNonTeam: Int = items.filter({
          item =>
            !item.isComplete.getOrElse(false) && !"team".equals(latestEstimateCurrency(item))
        }
        ).foldLeft(0)(incrementedEstimate);

        val n = (
                StatsLogEntry(
                  version = version.id,
                  when = version.when,
                  memo = version.backlog.memo,
                  team = amountTeam,
                  nonTeam = amountNonTeam,
                  done = amountComplete
                ),
                version)

        allResults += n

    })

    val results = allResults.filter(_._1.when <= until)

    val (latest, _) = results.head

    results.filter {
      next =>
        val (item, _) = next
        val includeBecauseItsLast = (includeCurrentState && item.version == latest.version)
        val includeBecauseItsNotWIP = item.memo != "work-in-progress"
        includeBecauseItsLast || includeBecauseItsNotWIP
    }

  }

  def filterBacklogHistory(backlogId: String, fn: (BacklogVersion) => Boolean): Seq[BacklogVersion] = {
    var results = ListBuffer[BacklogVersion]()

    scanBacklogHistory(backlogId, {
      next =>
        val matches = fn(next)
        if (matches) results += next
    })

    results.toSeq
  }

}