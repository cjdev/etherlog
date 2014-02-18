package com.cj.etherlog
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.{Id, As}
import com.cj.etherlog.chart.GoalData

case class Estimate(
    val id:String,
    val currency:String,
    val value:Int,
    val when:Long
    )
    
case class Item(
    val id:String,
    val isComplete:Option[Boolean] = Some(false),
    val name:String,
    val kind:String,
    val estimates:Option[Seq[Estimate]],
    val when:Option[Long] = None
){
  def bestEstimate() = estimates match {
      case Some(e) => {
          val latestEstimate = e.maxBy(_.when)
          if(latestEstimate!=null){
            Some(latestEstimate.value);
          }else{
            None
          }
      }
      case None => None
    }
}

case class Backlog (
    val id:String,
    val name:String,
    val memo:String,
    val projectedVelocity:Option[Int] = None,
    val items:Seq[Item]){
 
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
  
  def goalData() = {
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
        GoalData(description=name, points=totalSize-amount, when=goal.when)
      }.toSeq
  }
  
}