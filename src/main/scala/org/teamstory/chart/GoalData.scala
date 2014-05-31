package org.teamstory.chart

case class GoalData(
    description:String, 
    points:Int, 
    when:Option[Long] = None,
    whenForReal:Option[Long] = None)