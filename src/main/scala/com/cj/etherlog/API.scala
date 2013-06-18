package com.cj.etherlog.api { 
  
    case class HistoryItem (version:String, when:Long, memo:String)
    
    case class StatsLogEntry (version:String, when:Long, memo:String, todo:Int, done:Int){
        def total = done + todo
    }
    
    case class BacklogListEntry (id:String, name:String)
}
