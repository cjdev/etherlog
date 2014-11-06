package org.teamstory.http

import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.Jackson
import org.teamstory.TeamStory
import org.teamstory.Jackson._
import scala.collection.mutable.ListBuffer
import org.teamstory.datas.Data
import org.teamstory.Service
import org.teamstory.api.{BacklogListEntry, BacklogDto, BacklogStatusPatch}
import org.teamstory.datas.Backlog
import org.teamstory.datas.BacklogVersion
import java.util.UUID
import HttpUtils._
import org.teamstory.Clock
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
      
      val lockCheckPasses = dto.optimisticLockVersion match {
        case None => false
        case Some(version) => version == backlog.latestVersion
      }
      
      if(lockCheckPasses){
          service.saveBacklogUpdate(new Backlog(dto))
          get(req)
      }else{
          CONFLICT(Text(s"Mid-air collission? (given ${dto.optimisticLockVersion} but expected ${backlog.latestVersion}"))
      }
      
    }
}