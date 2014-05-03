package com.cj.etherlog.datas

import com.cj.etherlog.api._
import com.cj.etherlog.api.Item
import scala.Option.option2Iterable

case class BacklogVersion(
    val id:String,
    val when:Long = System.currentTimeMillis(),
    val isPublished:Boolean, 
    val previousVersion:String,
    val backlog:Backlog
){
  
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
    
    val added = change.backlog.items.filter{item=>
      !prev.backlog.item(item.id).isDefined
    }
    
    val removed = prev.backlog.items.filter{item=>
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
    
    def sumEstimates(items:Seq[Item]):Int = {
      val bestEstimates = items.flatMap(_.bestEstimate)
      val sum = bestEstimates.foldLeft(0)(_+_)
      sum
    }
    
    Delta(
        from = prev.versionNameAndTime,
        to = change.versionNameAndTime,
        added=sumEstimates(added), 
        removed=sumEstimates(removed), 
        finished=sumEstimates(finished), 
        reopened = sumEstimates(reopened))
    
  }
}