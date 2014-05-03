package com.cj.etherlog.http

import org.httpobjects._
import org.httpobjects.DSL._
import com.cj.etherlog.data.Data
import com.cj.etherlog.Jackson
import com.cj.etherlog.api.BacklogStatusPatch
import com.cj.etherlog.Etherlog
import com.cj.etherlog.Jackson._
import scala.collection.mutable.ListBuffer
import com.cj.etherlog.data.Data
import com.cj.etherlog.Service
import com.cj.etherlog.api.BacklogListEntry
import com.cj.etherlog.Backlog
import HttpUtils._

class BacklogsResource (data:Data, service:Service) extends HttpObject("/api/backlogs"){
    override def get(req:Request) = {
       val results = new ListBuffer[BacklogListEntry]()
       
       data.backlogs.scan{(id, backlog)=>
         results += data.toBacklogListEntry(backlog)
       }
      
       OK(JerksonJson(results))
    }
    
    override def post(req:Request) = {
      val backlogRecieved = Jackson.parseJson[Backlog](readAsStream(req.representation()));
      val newVersionId = service.createBacklog(backlogRecieved);
      CREATED(Location("/api/backlogs/" + newVersionId))
    }
}