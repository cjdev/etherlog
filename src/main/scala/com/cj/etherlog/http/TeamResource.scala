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
import com.cj.etherlog.api.TeamDto

class TeamResource (data:Data, clock:Clock) extends HttpObject("/api/team/{id}"){
  
    override def get(req:Request) = {
     val team = data.teams.get(req.path.valueFor("id"))
     
     if(team==null){
         NOT_FOUND
     }else{
         OK(Jackson.JerksonJson(team))
     }
    }
    
    override def put(req:Request) = {
     val id = req.path.valueFor("id")
     val team = Jackson.parse[TeamDto](req.representation())
     
     if(data.teams.contains(id)){
       val updated = team.copy(id=id)
       
       val today = clock.now.toDateTime().toYearMonthDay().toString();
       
       val currentIteration = team.iterations.maxBy(_.start)
       
       val newIterations = team.iterations.filter(_.start==null)
       
       if(newIterations.size>1){
         BAD_REQUEST(Text("You can't have more than one iteration at a time"))
       }else if(today == currentIteration.start){
         BAD_REQUEST(Text("Today is already the start of an iteration"))
       }else {
           data.teams.put(id, team)
           get(req)
       }
       
     }else{
       BAD_REQUEST(Text("No such team: " + id))
     }
     
    }
}