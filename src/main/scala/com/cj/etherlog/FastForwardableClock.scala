package com.cj.etherlog

import org.joda.time.Instant

class FastForwardableClock (val enableTimeTravel:Boolean) extends Clock {
   var offset:Long = 0;
   
   def travelForwardXMillis(adjustment:Long){
     if(!enableTimeTravel) throw new Exception("Time travel is not enabled")
     FastForwardableClock.this.offset = FastForwardableClock.this.offset + adjustment
   }
   
   def now() = {
     new Instant(System.currentTimeMillis() + offset)
   }
}