package org.teamstory.http

import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.datas.Data
import org.teamstory.Jackson
import org.teamstory.api.BacklogStatusPatch
import org.teamstory.TeamStory
import org.teamstory.Jackson._
import org.teamstory.Service
import org.joda.time.Months
import scala.collection.mutable.ListBuffer
import org.joda.time.Instant
import org.joda.time.YearMonthDay
import org.teamstory.chart.ChartProjection
import org.teamstory.datas.BacklogVersion
import org.teamstory.chart.ChartOptions
import HttpUtils._
import org.teamstory.Clock
import org.joda.time.DateMidnight
import org.teamstory.chart.IterationStats
import org.teamstory.datas.BacklogVersion
import org.teamstory.chart.IterationStats
import org.teamstory.api._
import org.joda.time.DateTime
import org.apache.commons.io.IOUtils
import java.text.NumberFormat

class TeamIterationResource (data:Data, clock:Clock) extends HttpObject("/api/team/{id}/iteration"){
    
    override def get(req:Request) = {
     val id = req.path.valueFor("id")
     val team = data.teams.get(id)
     
     OK(Jackson.JerksonJson(team.iterations))
    }
    
    def parseIntIfAble(s:String) = {
      try{
        Some(s.toLong)
      }catch{
        case e:NumberFormatException => None
      }
    }
    
    override def post(req:Request) = {
     val id = req.path.valueFor("id")
     
     val str = IOUtils.toString(HttpUtils.readAsStream(req.representation())).trim()
     val now = new Instant(parseIntIfAble(str).getOrElse(clock.now.getMillis()))
     
     try{
     println("foo!")
     
     if(data.teams.contains(id)){
       val today = now.toDateTime().toYearMonthDay()
       val team = data.teams.get(id);
       val currentIteration = if(team.iterations.size>0) Some(team.iterations.maxBy(_.start)) else None
       
       println("max is " + currentIteration)
       println("today is " + today)
       
       if(currentIteration.isDefined && today == new DateTime(currentIteration.get.start).toYearMonthDay()){
         BAD_REQUEST(Text("Today is already the start of an iteration"))
       }else {
           data.teams.put(id, team.copy(iterations=(team.iterations.toList :+ IterationDto(start=clock.now.getMillis(), label=today.toString()))))
           get(req)
       }
       
     }else{
       BAD_REQUEST(Text("No such team: " + id))
     }
     }catch {
       case e:Throwable=>{
         e.printStackTrace(); 
         BAD_REQUEST(Text("oops!"))
       }
     }
    }
}