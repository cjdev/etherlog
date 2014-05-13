package com.cj.etherlog.api { 
    
    case class GlobalConfig (defaultChartType:String = "default") 
    
    case class HistoryItem (version:String, when:Long, memo:String)
    
    case class StatsLogEntry (version:String, when:Long, memo:String, todo:Int, done:Int){
        def total = done + todo
    }
    
    case class BacklogListEntry (id:String, name:String, whenArchived:Option[Long])
     
    case class BacklogStatusPatch (archived:Boolean)
    
    case class VersionNameAndTime(
        id:String, 
        memo:String,
        when:Long)

    case class Delta (from:VersionNameAndTime, to:VersionNameAndTime, added:Int, removed:Int, finished:Int, reopened:Int)
    
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
    
    case class BacklogDto (
        val id:String,
        val name:String,
        val memo:String,
        val projectedVelocity:Option[Int] = None,
        val items:Seq[Item],
        val optimisticLockVersion:Option[String] = None)
}
