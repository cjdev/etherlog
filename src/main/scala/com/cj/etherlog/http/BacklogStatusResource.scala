package com.cj.etherlog.http

import org.httpobjects._
import org.httpobjects.DSL._
import com.cj.etherlog.data.Data
import com.cj.etherlog.Jackson
import com.cj.etherlog.api.BacklogStatusPatch
import com.cj.etherlog.Etherlog
import com.cj.etherlog.Jackson._
import HttpUtils._

class BacklogStatusResource(data:Data) extends HttpObject("/api/backlogs/{id}/status"){
    override def put(req:Request) = {
      val id = req.path().valueFor("id")
      val backlog = data.backlogs.get(id)
      val newStatus = Jackson.parseJson[BacklogStatusPatch](readAsStream(req.representation()));
      
      val whenArchived = (newStatus.archived, backlog.whenArchived) match {
        case (true, None)=>{
          Some(System.currentTimeMillis())
        }
        case (false, Some(millis)) => {
          None
        }
        case (_, whenArchived) => whenArchived
      }
      
      val newBacklog = backlog.copy(whenArchived=whenArchived)
      data.backlogs.put(id, newBacklog);
      
      OK(JerksonJson(data.toBacklogListEntry(newBacklog)))
    }
}