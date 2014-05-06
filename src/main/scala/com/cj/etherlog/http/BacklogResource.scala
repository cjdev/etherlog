package com.cj.etherlog.http

import org.httpobjects._
import org.httpobjects.DSL._
import com.cj.etherlog.Jackson
import com.cj.etherlog.Etherlog
import com.cj.etherlog.Jackson._
import scala.collection.mutable.ListBuffer
import com.cj.etherlog.datas.Data
import com.cj.etherlog.Service
import com.cj.etherlog.api.{BacklogListEntry, BacklogDto, BacklogStatusPatch}
import com.cj.etherlog.datas.Backlog
import com.cj.etherlog.datas.BacklogVersion
import java.util.UUID
import HttpUtils._
import com.cj.etherlog.Clock
import org.joda.time.Instant

class BacklogResource (data:Data, service:Service, clock:Clock) extends HttpObject("/api/backlogs/{id}"){
    
    override def get(req:Request) = {
      val id = req.path().valueFor("id")
      val backlog = data.backlogs.get(id)
      val currentVersion = data.versions.get(backlog.latestVersion);
      OK(JerksonJson(currentVersion.backlog.toDto(backlog.latestVersion)))
    }
    
    override def put(req:Request) = {
      val id = req.path().valueFor("id")
      val dto = Jackson.parseJson[BacklogDto](readAsStream(req.representation()));
      val backlog = data.backlogs.get(id);
      
      val newVersion = new BacklogVersion(
                          id = UUID.randomUUID().toString(),
                          when = clock.now.getMillis,
                          isPublished = false,
                          previousVersion = backlog.latestVersion,
                          backlog = new Backlog(dto))
      
      println("the time is now " + new Instant(newVersion.when))
      
      val lockCheckPasses = dto.optimisticLockVersion match {
        case None => false
        case Some(version) => version == backlog.latestVersion
      }
      
      if(lockCheckPasses){
        val updatedBacklog = backlog.copy(latestVersion = newVersion.id)
          data.versions.put(updatedBacklog.latestVersion, newVersion);
          data.backlogs.put(id, updatedBacklog)
          
          service.notifySubscribers(backlog)
          get(req)
      }else{
          CONFLICT(Text(s"Mid-air collission? (given ${dto.optimisticLockVersion} but expected ${backlog.latestVersion}"))
      }
      
    }
}