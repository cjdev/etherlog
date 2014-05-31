package org.teamstory.http
import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.datas.Data
import org.teamstory.Jackson
import org.teamstory.api.BacklogStatusPatch
import org.teamstory.TeamStory
import org.teamstory.Jackson._
import org.teamstory.datas.BacklogVersion
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormat
import org.joda.time.YearMonthDay
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