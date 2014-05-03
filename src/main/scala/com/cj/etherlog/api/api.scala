package com.cj.etherlog.api { 
  
    case class HistoryItem (version:String, when:Long, memo:String)
    
    case class StatsLogEntry (version:String, when:Long, memo:String, todo:Int, done:Int){
        def total = done + todo
    }
    
    case class BacklogListEntry (id:String, name:String, whenArchived:Option[Long])
     
    case class VersionNameAndTime(
        id:String, 
        memo:String,
        when:Long)

    case class Delta (from:VersionNameAndTime, to:VersionNameAndTime, added:Int, removed:Int, finished:Int, reopened:Int)
    
}
