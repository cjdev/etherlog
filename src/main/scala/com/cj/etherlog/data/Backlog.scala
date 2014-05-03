package com.cj.etherlog
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.{Id, As}
import com.cj.etherlog.chart.GoalData
import com.cj.etherlog.api.BacklogDto
import com.cj.etherlog.api.Item

case class Backlog (
    val id:String,
    val name:String,
    val memo:String,
    val projectedVelocity:Option[Int] = None,
    val items:Seq[Item]){
  
  def this(dto:BacklogDto) = {
    this(id=dto.id,
        name = dto.name,
        memo = dto.memo,
        projectedVelocity = dto.projectedVelocity,
        items = dto.items)
  }
  
  def toDto(optimisticLockVersion:String) = BacklogDto(id=id, name=name, memo=memo, projectedVelocity=projectedVelocity, items =items, optimisticLockVersion=Some(optimisticLockVersion))
  
  def totalSize() = items.size match {
      case 0=>0;
      case _=> items.foldLeft(0){(accum, item)=> item.bestEstimate.getOrElse(0) + accum}
    }
  
  def item(id:String) = items.find(_.id==id)
  
  def todo() = items.filter(_.kind!="goal").foldLeft(0){(total, item)=>
    val todo = if(item.isComplete.getOrElse(false)){
      0
    }else{
      item.bestEstimate.getOrElse(0)
    }
            total + todo
  }
  
  def goals = items.filter(_.kind=="goal")
  
  def goalHasBeenMet(goalId:String) = {
      val goal = goals.find(_.id == goalId).get 
      val goalPos = items.indexOf(goal)
      items.foldLeft(true){(accum, item)=>
          if(accum){
            if(item.kind == "goal"){
              true
            }else{
                val complete = item.isComplete.getOrElse(false)
                complete || items.indexOf(item) > goalPos
            }
          }else{
              false
          }
      }
  }
  
  def goalData(whenMeasured:Long) = {
      val goals = items.filter(_.kind=="goal");
      
      val goalsByName = goals.foldLeft(Map[String, Item]()){(accum, item) => accum.updated(item.name, item)}
      
      val estimatesByGoal = for(goal<-goals; item<-items; val goalPos = items.indexOf(goal); val itemPos = items.indexOf(item)) yield {
          val goalComplete = goalHasBeenMet(goal.id)
          if(item.kind != "goal" && (itemPos<goalPos || (!goalComplete && item.isComplete.getOrElse(false)))){
              val amount = item.bestEstimate match {
                  case Some(amount) => amount
                  case None => 0
              }
              (goal, amount)
          }else{
              (goal, 0)
          }
      }


      val totalsByGoalName = estimatesByGoal.foldLeft(Map[String, Int]()){(accum, item)=> 
          val (goal, itemAmount) = item;
          val amount = accum.getOrElse(goal.name, 0) + itemAmount
          
          accum.updated(goal.name,amount)
      };

      totalsByGoalName.map{entry=>
        val (name, amount) = entry
        val goal = goalsByName(name)
        
        
        val foo = projectedVelocity match {
          case Some(weeklyVelocity)=> {
            val pointsDone = totalSize - todo
            val pointsToGoal = amount - pointsDone
            val pointsPerWeek = weeklyVelocity
            val numWeeksToGoal = pointsToGoal.toDouble/pointsPerWeek.toDouble
            val numMillisInWeek = 1000 * 60 * 60 * 24 * 7;
            
            val millisWhenGoalComplete = (whenMeasured + (numMillisInWeek.toDouble * numWeeksToGoal)).toLong
            Some(millisWhenGoalComplete)
          }
          case None=>None
        }
        GoalData(description=name, points=totalSize-amount, when=goal.when, whenForReal=foo)
      }.toSeq
  }
  
}