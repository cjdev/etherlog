package com.cj.etherlog.api { 
  
    case class HistoryItem (val version:String, val when:Long, val memo:String)
    
    case class StatsLogEntry (val version:String, val when:Long, val memo:String, val todo:Int, val done:Int){
        def total = done + todo
    }
    
    case class BacklogListEntry (val id:String, val name:String)
}
