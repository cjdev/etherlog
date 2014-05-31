package org.teamstory.datas
import java.io.{File => Path}
import scala.collection.mutable.ListBuffer
import org.teamstory.api.BacklogListEntry
import org.teamstory.api.StatsLogEntry
import org.teamstory.api.Item
import org.teamstory.api.HistoryItem
import org.teamstory.api.GlobalConfig
import org.teamstory.api.TeamDto

class Data (val dataPath:Path){
    
    val errors = new Database[String](new Path(dataPath, "errors"))
    val backlogs = new Database[BacklogStatus](new Path(dataPath, "backlogs"))
    val versions = new Database[BacklogVersion](new Path(dataPath, "versions"))
    val config = new Database[GlobalConfig](new Path(dataPath, "config"))
    val teams = new Database[TeamDto](new Path(dataPath, "teams"))
    
    def setGlobalConfig(n:GlobalConfig) = {
      config.put("global", n)
    }
    
    def getGlobalConfig() = {
      if(config.contains("global")){
        config.get("global")
      }else{
        GlobalConfig()
      }
    }
    
    
    val versionsCache = scala.collection.mutable.Map[String, BacklogVersion]()
    
    def scanBacklogHistory(backlogId:String, fn:(BacklogVersion)=>Unit):Unit = {
      val backlog = backlogs.get(backlogId);
      var nextVersionId = backlog.latestVersion
      while(nextVersionId!=null){
        val version = versionsCache.get(nextVersionId) match {
          case Some(version)=>version
          case None=> versions.get(nextVersionId)
        }
        versionsCache.put(nextVersionId, version)
        fn(version)
        nextVersionId = version.previousVersion
      }
     }
     
    def getHistory(id:String):Seq[HistoryItem] = {
      val results = new ListBuffer[HistoryItem]()
      scanBacklogHistory(id, {version=>
        results += HistoryItem(version=version.id, when=version.when, memo=version.backlog.memo)
      }) 
      results.toSeq
    }
    
    def toBacklogListEntry(backlogStatus:BacklogStatus)= {
       val version = versions.get(backlogStatus.latestVersion)
       BacklogListEntry(id=backlogStatus.id, name=version.backlog.name, whenArchived=backlogStatus.whenArchived)
    }
    
        def buildStatsLog(id:String, until:Long, includeCurrentState:Boolean = false)= {
      val backlog = backlogs.get(id) 
      val allResults = new ListBuffer[(StatsLogEntry, BacklogVersion)]()
      
      def incrementedEstimate(tally:Int, item:Item) = {
          item.bestEstimate match {
          case Some(value) => {
              value + tally
          }
          case None => tally
          }
      }
      
      scanBacklogHistory(id, {version=>
            val items = version.backlog.items
            val amountComplete:Int = items.filter(_.isComplete.getOrElse(false)).foldLeft(0)(incrementedEstimate);
            val amountTodo:Int = items.filter(!_.isComplete.getOrElse(false)).foldLeft(0)(incrementedEstimate);
            
            val n = (StatsLogEntry(
                    version=version.id, 
                    when=version.when, 
                    memo=version.backlog.memo,
                    todo=amountTodo,
                    done=amountComplete), version)
            
            allResults += n
                        
      }) 
      
      var results = allResults.filter(_._1.when<=until)
      
      var (latest, _) = results.head
      
      results.filter{next=>
            var (item, _) = next
            var includeBecauseItsLast = (includeCurrentState && item.version == latest.version)
            var includeBecauseItsNotWIP = item.memo!="work-in-progress" 
            includeBecauseItsLast || includeBecauseItsNotWIP
      }
              
    }
        
    def filterBacklogHistory(backlogId:String, fn:(BacklogVersion)=>Boolean):Seq[BacklogVersion]  = {
      var results = ListBuffer[BacklogVersion]()
      
      scanBacklogHistory(backlogId, {next=>
          val matches = fn(next)
          if(matches) results += next
      })
      
      results.toSeq
    }
    
}