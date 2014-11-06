package org.teamstory.pivotal

import java.net.URL
import scala.collection.JavaConversions._
import org.teamstory.api._
import java.util.UUID
import scala.collection.mutable.ListBuffer
import org.teamstory.datas.Backlog

object PivotalSync {
    def pivotalProject2TeamStoryBacklog(stories:Seq[PT5Story], epics:Seq[PT5Epic], project:PT5Project, teamStoryId:String):Backlog = {
      Backlog(
            id=teamStoryId.toString,
            name=project.name,
            memo="work-in-progress",
            projectedVelocity = None,
            items= toItemList(stories, epics))
    }
    def main(args:Array[String]){
      
        val pivotalAPIKey = args(0)
        val pivotalProjectId = args(1)
        val teamStoryBaseUrl = args(2)
        val teamStoryId = args(3)
        val t = new PivotalTrackerV5ApiStub(pivotalAPIKey)
        
        while(true){
          
            val stories = t.getStories(pivotalProjectId)
            val epics = t.getEpics(pivotalProjectId)
            val project = t.getProject(pivotalProjectId)
                    
                    
            val backlog = pivotalProject2TeamStoryBacklog(stories, epics, project, teamStoryId)
                            
            val teamStory = new JacksonHttpTool()
            val backlogURL = s"${teamStoryBaseUrl}/api/backlogs/${teamStoryId}"
            val current = teamStory.getJson[BacklogDto](backlogURL)
            
            val update = backlog.toDto(current.optimisticLockVersion.get)
            if(update != current){
                println("Something updated!")
                teamStory.putJson(backlogURL, update)
            }else{
                println("No Update")
            }
            
            Thread.sleep(5000)
        }
    }
    
    def toItemList(stories:Seq[PT5Story], epics:Seq[PT5Epic]):Seq[Item] = {
      val items = ListBuffer[Item]()
      
      // add all the stories
      val storyItems = stories.map(toStory)
      items.addAll(storyItems)
      
      case class GoalEpic (goal:Item, epic:PT5Epic)
      
      // for each epic, put it:
      val goalEpics = epics.map{e=>GoalEpic(toGoal(e), e)}
      
      goalEpics.zipWithIndex.foreach{n=>
         val (GoalEpic(goal, epic), idx) = n
         val maybePrev = if(idx==0) None else Some(goalEpics(idx-1))
         
         val epicStories = stories.filter{s=>
             s.labels.find(_.id == epic.label.id).isDefined
         }
         if(epicStories.size>0){
             //   after the last related story, if applicable
           val idx = stories.indexOf(epicStories.last)
           items.insert(idx+1, goal)
         }else{
             //   or otherwise, after the previous epic
           val idx = maybePrev match {
             case Some(previous) => {items.indexOf(previous.goal) + 1}
             case None => 0
           }
           
           items.insert(idx, goal)
         }
      }
      
      // and insert a goal to represent the "icebox", as needed
      val maybeFirstIceboxStory = stories.find(_.state == unscheduled).headOption
      val goalIndexes = items.zipWithIndex.filter(_._1.kind=="goal").map(_._2)
      val maybeIdxOfLastGoal = if(goalIndexes.isEmpty){None}else{
          Some(goalIndexes.last)
      }
      val iceboxBoundaryGoal = Item(id="PTIcebox", name="End of Backlog / Start of Icebox", kind="goal", when=None, estimates=None)
      val maybeIdx = (maybeFirstIceboxStory, maybeIdxOfLastGoal) match {
        case (None, None)=>None
        case (None, Some(idxOfLastGoal)) => Some(idxOfLastGoal + 1)
        case (Some(firstIceboxStory), _) => {
          println(s"Placing before the first icebox story (${firstIceboxStory.name}")
          val storyItemIdx = stories.indexOf(firstIceboxStory)
          val storyItem = storyItems(storyItemIdx)
          val idxOfFirstIceboxStory = items.indexOf(storyItem)
          Some(idxOfFirstIceboxStory)
        }
      }
      maybeIdx match {
        case Some(idx)=>items.insert(idx, iceboxBoundaryGoal)
        case None=>
      }
        
        
      
      
      items.toSeq
    }
    def toGoal(pt:PT5Epic) = {
      Item(
        id =  "PT" + pt.id.toString,
        isComplete = None,
        inProgress = None,
        name =  pt.name + "\n" + pt.description,
        kind = "goal",
        estimates = None,
        when = None    
      )
    }
    def toStory(pt:PT5Story) = {
      
      val maybeEstimates = pt.estimate match {
        case null => None
        case value => Some(Seq(Estimate(
            id = "",
            currency = "team",
            value = value.intValue,
            when = pt.updated_at.millis
        )))
      }
      
      val description = pt.description match {
        case null => pt.name
        case text => pt.name + "\n" + text
      }
      
      Item(
        id =  "PT" + pt.id.toString,
        isComplete = Some(pt.isComplete),
        inProgress = Some(pt.isInProgress),
        name =  description,
        kind = "story",
        estimates = maybeEstimates,
        when = None    
      )
    }
}