package com.cj.etherlog.http

import org.httpobjects._
import org.httpobjects.DSL._
import com.cj.etherlog.data.Data
import com.cj.etherlog.Jackson
import com.cj.etherlog.Etherlog
import com.cj.etherlog.Jackson._
import scala.collection.mutable.ListBuffer
import com.cj.etherlog.data.Data
import com.cj.etherlog.Service
import com.cj.etherlog.api.{BacklogListEntry, BacklogDto, BacklogStatusPatch}
import com.cj.etherlog.Backlog
import com.cj.etherlog.data.BacklogVersion
import java.util.UUID
import HttpUtils._

class BacklogResource (data:Data, service:Service) extends HttpObject("/api/backlogs/{id}"){
    
    override def get(req:Request) = {
      val id = req.path().valueFor("id")
      val backlog = data.backlogs.get(id)
      val currentVersion = data.versions.get(backlog.latestVersion);
      OK(JerksonJson(currentVersion.backlog.toDto))
    }
    
    override def put(req:Request) = {
      val id = req.path().valueFor("id")
      val dto = Jackson.parseJson[BacklogDto](readAsStream(req.representation()));
      val backlog = data.backlogs.get(id);
      
      val newVersion = new BacklogVersion(
                          id = UUID.randomUUID().toString(),
                          when = System.currentTimeMillis(),
                          isPublished = false,
                          previousVersion = backlog.latestVersion,
                          backlog = new Backlog(dto))
      
      val updatedBacklog = backlog.copy(latestVersion = newVersion.id)
      
      data.versions.put(updatedBacklog.latestVersion, newVersion);
      data.backlogs.put(id, updatedBacklog)

      service.notifySubscribers(backlog)
      get(req)
    }
}