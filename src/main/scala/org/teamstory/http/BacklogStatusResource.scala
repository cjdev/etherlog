package org.teamstory.http

import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.datas.Data
import org.teamstory.Jackson
import org.teamstory.api.BacklogStatusPatch
import org.teamstory.TeamStory
import org.teamstory.Jackson._
import HttpUtils._
import org.teamstory.Clock

class BacklogStatusResource(data:Data, clock:Clock) extends HttpObject("/api/backlogs/{id}/status"){
    override def put(req:Request) = {
      val id = req.path().valueFor("id")
      val backlog = data.backlogs.get(id)
      val newStatus = Jackson.parseJson[BacklogStatusPatch](readAsStream(req.representation()));
      
      val whenArchived = newStatus.archived match {
        case Some(isArchived)=> calculateWhenArchivedUpdate(isArchived, backlog.whenArchived)
        case None=>backlog.whenArchived 
      }
      
      val newBacklog = backlog.copy(
                          whenArchived=whenArchived,
                          pivotalTrackerLink=newStatus.pivotalTrackerLink)
                          
      if(newBacklog.pivotalTrackerLink .isDefined){
        println("Linking to pivotal tracker: " + newBacklog.pivotalTrackerLink )
      }
      data.backlogs.put(id, newBacklog);
      
      OK(JerksonJson(data.toBacklogListEntry(newBacklog)))
    }
    
    private def calculateWhenArchivedUpdate(newArchivedStatus:Boolean, oldArchivedStatus:Option[Long]):Option[Long] = {
      (newArchivedStatus, oldArchivedStatus) match {
        case (true, None)=>{
          Some(clock.now.getMillis)
        }
        case (false, Some(millis)) => {
          None
        }
        case (_, whenArchived) => whenArchived
      }
    }
}