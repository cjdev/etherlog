package org.teamstory.http

import org.teamstory.FastForwardableClock
import org.teamstory.Clock
import org.httpobjects.HttpObject
import org.httpobjects.DSL._
import org.httpobjects.Request
import org.teamstory.DurationFormat
import java.io.ByteArrayOutputStream
import org.teamstory.FastForwardableClock

class TimeTravelResource (val clock:FastForwardableClock) extends HttpObject("/api/clock") {
    override def get(request:Request) = OK(Text(clock.now.getMillis().toString))
    override def put(request:Request) = {
      val out = new ByteArrayOutputStream
      request.representation().write(out)
      out.flush()
      
      val textIn = new String(out.toByteArray())
      println("got this: " + textIn)
      val adjustmentInMillis = DurationFormat.MillisecondsFormat.parse(textIn)
      clock.travelForwardXMillis(adjustmentInMillis)
      println("Clock is now " + clock.now);
      get(request)
    }
    
}