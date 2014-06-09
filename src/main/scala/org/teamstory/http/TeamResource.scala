package org.teamstory.http

import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.datas.{Data, DataImpl, BacklogVersion}
import org.teamstory.Jackson
import org.teamstory.api.{IterationDto, BacklogStatusPatch, TeamDto}
import org.teamstory.TeamStory
import org.teamstory.Jackson._
import org.teamstory.Service
import org.joda.time.Months
import scala.collection.mutable.ListBuffer
import org.joda.time.Instant
import org.joda.time.YearMonthDay
import org.teamstory.chart.ChartProjection
import org.teamstory.chart.ChartOptions
import HttpUtils._
import org.teamstory.Clock
import org.joda.time.DateMidnight
import org.teamstory.chart.IterationStats
import org.teamstory.chart.IterationStats

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
       
       val newIterations = team.iterations.filter({x: IterationDto => !Option(x.start).isDefined})
       
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