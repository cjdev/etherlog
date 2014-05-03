package com.cj.etherlog.http
import org.httpobjects._
import org.httpobjects.DSL._
import com.cj.etherlog.data.Data
import com.cj.etherlog.Jackson
import com.cj.etherlog.api.BacklogStatusPatch
import com.cj.etherlog.Etherlog
import com.cj.etherlog.Jackson._
import com.cj.etherlog.data.BacklogVersion
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormat
import org.joda.time.YearMonthDay
import com.cj.etherlog.data.Data
import java.util.UUID
import HttpUtils._

class ErrorsResource (data:Data) extends HttpObject("/api/errors"){
  
    override def post(req:Request) = {
      val errorId = UUID.randomUUID().toString();
      val error = asString(readAsStream(req.representation()))
      
      data.errors.put(errorId, error)
      
      CREATED(Location("/api/errors/" + errorId))
    }
}