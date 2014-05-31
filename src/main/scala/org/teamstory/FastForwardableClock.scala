package org.teamstory

import org.joda.time.Instant
import org.teamstory.datas.Data

class FastForwardableClock (val configDb:Data, val enableTimeTravel:Boolean) extends Clock {
   val alreadyInTheFuture = configDb.getGlobalConfig.clockOffset > 0
   var offset:Long = configDb.getGlobalConfig.clockOffset
   
   def travelForwardXMillis(adjustment:Long){
     if(alreadyInTheFuture || enableTimeTravel) {
         offset = offset + adjustment
         configDb.setGlobalConfig(configDb.getGlobalConfig.copy(clockOffset=offset))
     }else{
       throw new Exception("Time travel is not enabled")
     }
   }
   
   def now() = {
     new Instant(System.currentTimeMillis() + offset)
   }
}