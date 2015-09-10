package org.teamstory.http

import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.datas.Data
import org.teamstory.datas.Backlog
import org.teamstory.Jackson
import org.teamstory.api.BacklogStatusPatch
import org.teamstory.TeamStory
import org.teamstory.Jackson._
import scala.collection.mutable.ListBuffer
import org.teamstory.Service
import org.teamstory.api.BacklogListEntry

import HttpUtils._

class BacklogsResource (data:Data, service:Service) extends HttpObject("/api/backlogs"){
    override def get(req:Request) = {
       val results = new ListBuffer[BacklogListEntry]()
       
       data.backlogs.scan{(id, backlog)=>
         results += data.toBacklogListEntry(backlog)
       }
      
       OK(JacksonJson(results))
    }
    
    override def post(req:Request) = {
      val backlogRecieved = Jackson.parseJson[Backlog](readAsStream(req.representation()));
      val newVersionId = service.createBacklog(backlogRecieved);
      CREATED(Location("/api/backlogs/" + newVersionId))
    }
}