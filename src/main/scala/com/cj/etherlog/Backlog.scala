package com.cj.etherlog

import org.codehaus.jackson.annotate.JsonTypeInfo
import org.codehaus.jackson.annotate.JsonTypeInfo.Id
import org.codehaus.jackson.annotate.JsonTypeInfo.As

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
    val estimates:Option[List[Estimate]]
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
    val items:List[Item]){
 
  def totalSize() = items.size match {
      case 0=>0;
      case _=> items.foldLeft(0){(accum, item)=> item.bestEstimate.getOrElse(0) + accum}
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
                println(item.name + " is complete " + complete)
                complete || items.indexOf(item) > goalPos
            }
          }else{
              false
          }
      }
  }
  
  def goalLines() = {
      val goals = items.filter(_.kind=="goal");
      
      def shortName(n:String) = n.lines.next
      
      val estimatesByGoal = for(goal<-goals; item<-items; val goalPos = items.indexOf(goal); val itemPos = items.indexOf(item)) yield {
          val goalComplete = goalHasBeenMet(goal.id)
          println(shortName(goal.name) + " is complete " +  goalComplete)
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
              accum.updated(goal.name, amount)
      };

      println("Goal lines: " );

      totalsByGoalName.foreach{i=>
          val (goalName, amt) = i
          println("   " + shortName(goalName) + ", qty " + amt + "(" + (totalSize-amt) + ")")
      }

      totalsByGoalName.values.map(totalSize-_).toList
  }
  
}