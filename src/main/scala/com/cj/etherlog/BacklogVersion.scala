package com.cj.etherlog

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
}