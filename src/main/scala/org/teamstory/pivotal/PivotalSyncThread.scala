package org.teamstory.pivotal

import org.teamstory.datas.Data
import org.teamstory.Service
import java.util.UUID
import org.joda.time.DateTime
import org.joda.time.Instant
import org.joda.time.Hours
import org.teamstory.Clock
import org.teamstory.datas.BacklogStatus
import org.joda.time.Seconds
import org.joda.time.Duration

class PivotalSyncThread(data:Data, service:Service, clock:Clock, timeToWaitBeforeAutoPublishing:Duration = Hours.FOUR.toStandardDuration()) extends Thread {
    
    private var lastSeenVersionsByProjectId = Map[String, Int]()
    
    override def run(){
      while(true){
        val ptLinkedBacklogs = data.backlogs.filter{(id, b)=>b.pivotalTrackerLink.isDefined}
    
        ptLinkedBacklogs.foreach{backlog=>
          try{
            getLatestWorkInProgressFromPivotalTracker(backlog)
            autoPublishIfNeeded(backlog.id, clock.now);
          }catch {
            case e:Exception => e.printStackTrace();
          }
        }
        println(getClass.getSimpleName + ": sleeping")
        Thread.sleep(15000)
      }
    }

    private def getLatestWorkInProgressFromPivotalTracker(backlog:BacklogStatus){
      val pivotal = new PivotalTrackerV5ApiStub(backlog.pivotalTrackerLink.get.apiKey)
      val projectId = backlog.pivotalTrackerLink.get.projectId.toString
      val project = pivotal.getProject(projectId)
      val lastSeenVersion = lastSeenVersionsByProjectId.getOrElse(projectId, -1)
      if(lastSeenVersion!=project.version){
          println(s"Pivotal project version changed from $lastSeenVersion to ${project.version}")
          val newBacklog = PivotalSync.pivotalProject2TeamStoryBacklog(
                  stories = pivotal.getStories(projectId), 
                  epics=pivotal.getEpics(projectId),
                  project=project, 
                  teamStoryId=backlog.id)
          val currentVersion = data.versions.get(data.backlogs.get(backlog.id).latestVersion)
          if(currentVersion.backlog != newBacklog){
              println("Something changed in pivotal")
              service.saveBacklogUpdate(newBacklog)
          }
          lastSeenVersionsByProjectId += (projectId -> project.version)
      }
    }
    
    private def autoPublishIfNeeded(backlogId:String, now:Instant) {
      val backlog = data.backlogs.get(backlogId)
      val latestVersion = data.versions.get(backlog.latestVersion)
      val thresholdForAutoPublish = new Instant(latestVersion.when).plus(timeToWaitBeforeAutoPublishing)
      
      if(!latestVersion.isPublished){
        if(now.isAfter(thresholdForAutoPublish)){
          println(s"""It's been more than $timeToWaitBeforeAutoPublishing since the last change to $backlogId ('${latestVersion.backlog.name}'), and the changes haven't been published.  Publishing now.""" )
          service.saveBacklogUpdate(latestVersion.backlog.copy(memo="Auto save ... last change made " + new DateTime(latestVersion.when)))
        }else{
          println(s"""There are unpublished changes to $backlogId ('${latestVersion.backlog.name}'); I'll auto publish them at $thresholdForAutoPublish if theyre' not published by then.""")
        }
      }
    }
  }