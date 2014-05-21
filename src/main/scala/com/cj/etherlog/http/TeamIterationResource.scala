package com.cj.etherlog.http

import org.httpobjects._
import org.httpobjects.DSL._
import com.cj.etherlog.datas.Data
import com.cj.etherlog.Jackson
import com.cj.etherlog.api.BacklogStatusPatch
import com.cj.etherlog.Etherlog
import com.cj.etherlog.Jackson._
import com.cj.etherlog.Service
import org.joda.time.Months
import scala.collection.mutable.ListBuffer
import org.joda.time.Instant
import org.joda.time.YearMonthDay
import com.cj.etherlog.chart.ChartProjection
import com.cj.etherlog.datas.BacklogVersion
import com.cj.etherlog.chart.ChartOptions
import HttpUtils._
import com.cj.etherlog.Clock
import org.joda.time.DateMidnight
import com.cj.etherlog.chart.IterationStats
import com.cj.etherlog.datas.BacklogVersion
import com.cj.etherlog.chart.IterationStats
import com.cj.etherlog.api._
import org.joda.time.DateTime

class TeamIterationResource (data:Data, clock:Clock) extends HttpObject("/api/team/{id}/iteration"){
    
    override def get(req:Request) = {
     val id = req.path.valueFor("id")
     val team = data.teams.get(id)
     
     OK(Jackson.JerksonJson(team.iterations))
    }
    
    override def post(req:Request) = {
     val id = req.path.valueFor("id")
     try{
     println("foo!")
     
     if(data.teams.contains(id)){
       val today = clock.now.toDateTime().toYearMonthDay()
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