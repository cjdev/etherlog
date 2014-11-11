package org.teamstory.datas

import org.teamstory.api._
import org.teamstory.api.Item
import scala.Option.option2Iterable
import org.teamstory.Jackson
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

object BacklogVersion {
  val WORK_IN_PROGRESS_LABEL="work-in-progress"
}

@JsonIgnoreProperties(ignoreUnknown = true)
case class BacklogVersion(
    val id:String,
    val when:Long,
    val previousVersion:String,
    val backlog:Backlog
){
  
  def isPublished = backlog.memo  != BacklogVersion.WORK_IN_PROGRESS_LABEL
  
  def projectedEnd() = {
      backlog.projectedVelocity match {
        case Some(pointsPerWeek) => {
            val todo = backlog.todo
            
            val weeks = todo.toDouble/pointsPerWeek.toDouble
            
            val millis = (weeks * 7 * 24 * 60 * 60 * 1000).longValue
            
            Some(when + millis)
        }
        case None=>None
      }
  }
  
  def versionNameAndTime = VersionNameAndTime(id, backlog.memo, when)
    
  def delta(prev:BacklogVersion) = {
    val change = this
    
    if(prev.when > change.when) throw new Exception("time ran backwards: " + prev.when + " to " + change.when)
    
    val added = change.backlog.items.filter(_.isStoryOrEpic).filter{item=>
      !prev.backlog.item(item.id).isDefined
    }
    
    val removed = prev.backlog.items.filter(_.isStoryOrEpic).filter{item=>
      !change.backlog.item(item.id).isDefined
    }
    
    val finished = change.backlog.items.filter{item=>
       val isComplete = item.isComplete.getOrElse(false)
       prev.backlog.item(item.id) match {
         case Some(old) => 
           val wasComplete = old.isComplete.getOrElse(false)
           (!wasComplete) && isComplete
         case None => isComplete
       }
    }
    
    
    val reopened = change.backlog.items.filter{item=>
       prev.backlog.item(item.id) match {
         case Some(old) => 
           val isComplete = item.isComplete.getOrElse(false)
           val wasComplete = old.isComplete.getOrElse(false)
           (wasComplete) && !isComplete
         case None => false
       }
    }
    
    def sumEstimates(items:Seq[Item]):ItemGroupSynopsis = {
      val bestEstimates = items.flatMap(_.bestEstimate)
      val sum = bestEstimates.foldLeft(0)(_+_)
      ItemGroupSynopsis(items.map(_.toIdAndShortName), sum)
    }
    
    val allItemIds = (change.backlog.items.map(_.id).toList ::: prev.backlog.items.map(_.id).toList).distinct
    case class ItemDelta(before:Option[Item], after:Option[Item]){
       def notNew = this match {
         case ItemDelta(Some(_), Some(_)) => true
         case _ => false
       }
       def wasReopened = {
         if(notNew){
             val isComplete = after.get.isComplete.getOrElse(false)
             val wasComplete = before.get.isComplete.getOrElse(false)
             (wasComplete) && !isComplete
         }else{
           false
         }
       }
       
       def estimateChanged = this match {
         case ItemDelta(Some(b), Some(a)) => a.bestEstimate.getOrElse(0) != b.bestEstimate.getOrElse(0)
         case _ => false
       }
       
       def increase = this match {
         case ItemDelta(Some(b), Some(a)) => a.bestEstimate.getOrElse(0) - b.bestEstimate.getOrElse(0)
         case _ => 0
       }
    }
    val beforeAndAfterItems = allItemIds.map{id=>
        ItemDelta(prev.backlog.item(id), change.backlog.item(id))
    }
//    println(Jackson.jackson.writerWithDefaultPrettyPrinter().writeValueAsString(beforeAndAfterItems))
    val changes = beforeAndAfterItems.filter(_.estimateChanged).filter(!_.wasReopened)
    val estimateIncreases = changes.map(_.increase).foldLeft(0)((accum, n)=>accum + n)
    
    val changedItems = changes.map{d=>d.after.get.toIdAndShortName}
    
    Delta(
        from = prev.versionNameAndTime,
        to = change.versionNameAndTime,
        added=sumEstimates(added), 
        removed=sumEstimates(removed), 
        finished=sumEstimates(finished), 
        reopened = sumEstimates(reopened),
        reestimated = ItemGroupSynopsis(changedItems, estimateIncreases))
    
  }
}