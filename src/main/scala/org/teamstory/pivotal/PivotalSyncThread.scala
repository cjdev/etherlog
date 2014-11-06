package org.teamstory.pivotal

import org.teamstory.datas.Data
import org.teamstory.Service

class PivotalSyncThread(data:Data, service:Service) extends Thread {
    
    override def run(){
      var lastSeenVersionsByProjectId = Map[String, Int]()
      while(true){
        val ptLinkedBacklogs = data.backlogs.filter{(id, b)=>b.pivotalTrackerLink.isDefined}
    
        ptLinkedBacklogs.foreach{backlog=>
          try{
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
                        backlog.id)
                val currentVersion = data.versions.get(data.backlogs.get(backlog.id).latestVersion)
                if(currentVersion.backlog != newBacklog){
                    println("Something changed in pivotal");
                    service.saveBacklogUpdate(newBacklog)
                }
                lastSeenVersionsByProjectId += (projectId -> project.version)
            }
          }catch {
            case e:Exception => e.printStackTrace();
          }
        }
        println(getClass.getSimpleName + ": sleeping")
        Thread.sleep(15000)
      }
    }
    
  }