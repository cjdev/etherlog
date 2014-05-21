package com.cj.etherlog.http

import org.httpobjects.DSL._
import org.httpobjects.HttpObject
import org.httpobjects.Request
import com.cj.etherlog.Jackson
import com.cj.etherlog.api.TeamDto
import com.cj.etherlog.datas.Data
import java.util.UUID

class TeamsResource (data:Data) extends HttpObject("/api/team"){
  
    override def get(get:Request) = OK(Jackson.JerksonJson(data.teams.toStream.toList))
    
    override def post(req:Request) = {
     val team = Jackson.parse[TeamDto](req.representation())
     
     val maybeExistingTeamWithSameName = data.teams.toStream.find(_.name==team.name)
     
     maybeExistingTeamWithSameName match {
       case Some(_) => BAD_REQUEST(Text("There is already a team named " + team.name))
       case None => {
         val record = team.copy(id = UUID.randomUUID().toString(), iterations=Seq())
         data.teams.put(record.id, record)
         CREATED(Location("/api/team/" + record.id))
       }
     }
    }
}