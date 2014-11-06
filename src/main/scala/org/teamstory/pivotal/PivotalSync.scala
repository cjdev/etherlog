package org.teamstory.pivotal

import org.teamstory.Jackson
import java.net.URL
import org.apache.http.impl.client._
import org.apache.http._
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.client.methods._
import scala.collection.JavaConversions._
import org.apache.http.message.BasicHeader
import com.fasterxml.jackson.annotation._
import scala.annotation.meta.getter
import org.teamstory.api._
import java.util.UUID
import scala.collection.mutable.ListBuffer
import org.joda.time.DateTimeZone.UTC

/**
 * A representation of a particular date and time. 
 * 
 * In general, datetime values can be expressed in one of two ways. They may be a string encoded according to the ISO 8601 
 * standard, like "2013-04-30T04:25:15Z". Or they will be an integer number of milliseconds since the beginning of the 
 * epoch, like 1367296015000. When supplying a parameter value that is a datetime, the client may use either format and 
 * Tracker will determine which is being used dynamically. 
 * 
 * By default, datetime values included in a response will be ISO 8601 strings. There are a handful of responses in which 
 * resources have datetime attributes which are only able to be formatted in one of these ways. This fact is noted in the 
 * descriptions of those particular attributes. For the majority, to cause dates to be formatted as millisecond offsets 
 * instead, supply the parameter date_format=millis in the query string of the request URL.
 */
object PT5DateTime {
  // e.g. 2014-11-06T04:47:06Z
  val pattern = org.joda.time.format.DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ss'Z'")
  @JsonCreator
  def deserialize(value:String) = {
    new PT5DateTime(value)
  }
}
case class PT5DateTime(@(JsonValue @getter) val value:String) {
  def millis:Long = PT5DateTime.pattern.parseDateTime(value).withZoneRetainFields(UTC).getMillis()
}

@JsonIgnoreProperties(ignoreUnknown=true)
case class PT5Label(
    
        /**
         * Database id of the label. This field is read only. This field is always returned.
         */
        id:Int, 
        /**
         * id of the project. This field is read only.
         */
        project_id:Int,
        /**
         * Required  —  The label's name.
         * string[255]
         */
        name:String,
        
        /**
         * Creation time. This field is read only.
         */
        created_at:PT5DateTime, 

        /**
         * Time of last update. This field is read only.
         */
        updated_at:PT5DateTime,
        
        /**
         * The type of this object: label. This field is read only. 
         */
        kind:String
)

@JsonIgnoreProperties(ignoreUnknown=true)
case class PT5Story(
    /**
     * Database id of the story. This field is read only. This field is always returned.
     */
    id:Int, 
    /**
     * id of the project
     */
    project_id:Int,
    /**
     * Required On Create  —  Name of the story. This field is required on create.
     *  string[5000]
     */
    name:String, 
    
    /**
     * In-depth explanation of the story requirements.
     * string[20000] 
     */
    description:String,
    
    /**
     * Type of story.
     *   enumerated string
     *   Valid enumeration values: feature, bug, chore, release
     */
    story_type:String,

    /**
     * Story's state of completion
     *  enumerated string 
     *  Valid enumeration values: accepted, delivered, finished, started, rejected, planned, unstarted, unscheduled
     */
    current_state:String,
    
    /**
     * estimate 
     * float
     * Point value of the story.
     */
    estimate:BigDecimal,
    
    /**
     * datetime 
     * Acceptance time.
     */
    accepted_at:PT5DateTime,
    
    /**
     * deadline datetime 
     * Due date/time (for a release-type story).
     * 
     */
    deadline:PT5DateTime,
    
    /**
     * The id of the person who requested the story. In API responses, this attribute may be requested_by_id or requested_by.
     */
    requested_by_id:Int,
    
    /**
     * The id of the person who owns the story. In API responses, this attribute may be owned_by_id or owned_by.
     */
    owned_by_id:Int,
    
    /**
     * IDs of the current story owners. By default this will be included in responses as an array of nested structures, using the key owners. In API responses, this attribute may be owner_ids or owners.
     */
    owner_ids:Seq[Int],
    
    /**
     * IDs of labels currently applied to story. By default this will be included in responses as an array of nested structures, using the key labels. In API responses, this attribute may be label_ids or labels.
     */
    label_ids:Seq[Int],
    labels:Seq[PT5Label],
    
    
    /**
     * IDs of tasks currently on the story. This field is writable only on create. This field is excluded by default. In API responses, this attribute may be task_ids or tasks.
     */
    task_ids:Seq[Int],
    
    /**
     * IDs of people currently following the story. This field is excluded by default. In API responses, this attribute may be follower_ids or followers.
     */
    follower_ids:Seq[Int],
    
    /**
     * IDs of comments currently on the story. This field is writable only on create. This field is excluded by default. In API responses, this attribute may be comment_ids or comments.
     */
    comment_ids:Seq[Int],
    
    /**
     * Creation time. This field is writable only on create.
     */
    created_at:PT5DateTime,
    
    /**
     * Time of last update. This field is read only.
     */
    updated_at:PT5DateTime,
    
    /**
     * ID of the story that the current story is located before. Null if story is last one in the project. This field is excluded by default.
     */
    before_id:Int,
    
    /**
     * ID of the story that the current story is located after. Null if story is the first one in the project. This field is excluded by default.
     */
    after_id:Int, 
    
    /**
     * ID of the integration API that is linked to this story. In API responses, this attribute may be integration_id or integration.
     */
    integration_id:Int,
  
    /**
     * The integration's specific ID for the story. (Note that this attribute does not indicate an association to another resource.)
     * string[255] 
     */
    external_id:String,
    
    /**
     * The url for this story in Tracker. This field is read only.
     */
    url:String, 

    /**
     * The type of this object: story. This field is read only.
     */
    kind:String
){
  
  def isComplete = state  match {
      case `accepted` => true
      case `delivered` => false
      case `finished` => true
      case `started` => false
      case `rejected` => false
      case `planned` => false
      case `unstarted` => false
      case `unscheduled` => false
    }
  
  def isInProgress = state  match {
      case `accepted` => false
      case `delivered` => false
      case `finished` => false
      case `planned` => false
      case `unstarted` => false
      case `unscheduled` => false
      case `started` => true
      case `rejected` => true
    }
  
  def state = PT5StoryState.valueOf(current_state).get
}

sealed trait PT5StoryState 
case object accepted extends PT5StoryState
case object delivered extends PT5StoryState
case object finished extends PT5StoryState
case object started extends PT5StoryState
case object rejected extends PT5StoryState
case object planned extends PT5StoryState
case object unstarted extends PT5StoryState
case object unscheduled extends PT5StoryState
object PT5StoryState{
  val values = Seq(
    accepted,
    delivered,
    finished,
    started,
    rejected,
    planned,
    unstarted,
    unscheduled)
    
    def valueOf(v:String):Option[PT5StoryState] = {
      val name = v + "$"
      values.find{v=>
          v.getClass().getSimpleName()==name
      }
    }
}


@JsonIgnoreProperties(ignoreUnknown=true)
case class PT5Epic (
    /**
     * Database id of the epic. This field is read only. This field is always returned.
     */
    id:Int,
    
    /**
     * id of the project.
     */
    project_id:Int,
    
    /**
     * name string[5000] 
     * Required On Create  —  Name of the epic. This field is required on create.
     */
    name:String,
    
    /**
     * id of the epic's label. By default this will be included in responses as a nested structure, using the key label. In API responses, this attribute may be label_id or label.
     */
    label_id:Int,
    label:PT5Label,
    
    /**
     *  In-depth explanation of the epic's goals, scope, etc.
     *  string[20000] 
     */
    description:String,

    /**
     * IDs of comments currently on the epic. This field is writable only on create. This field is excluded by default. In API responses, this attribute may be comment_ids or comments.
     */
    comment_ids:Seq[Int],

    /**
     * IDs of people currently following the story. This field is excluded by default. In API responses, this attribute may be follower_ids or followers.
     */
    follower_ids:Seq[Int],
    
    /**
     * Creation time. This field is read only.
     */
    created_at:PT5DateTime,
    
    /**
     * Time of last update. This field is read only.
     */
    updated_at:PT5DateTime,
    
    /**
     * The url for this epic in Tracker. This field is read only
     */
    url:String,
    
    /**
     * The type of this object: epic. This field is read only.
     */
    kind:String

)



@JsonIgnoreProperties(ignoreUnknown=true)
case class PT5Project (
    /**
     * Database id of the project. This field is read only. This field is always returned.
     */
    id:Int,
    
    /**
     * Required On Create  —  The name of the project. This field is required on create.
     * string[50] 
     */
    name:String
/**
 * 
version int 
 —  A counter that is incremented each time something is changed within a project. The project version is used to track whether a client is 'up to date' with respect to the current content of the project on the server, and to identify what updates have to be made to the client's local copy of the project (if it stores one) to re-synchronize it with the server. This field is read only.
 
iteration_length int 
 —  The number of weeks in an iteration.
 
week_start_day enumerated string 
 —  The day in the week the project's iterations are to start on.
Valid enumeration values: Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
 
point_scale string[255] 
 —  The specification for the "point scale" available for entering story estimates within the project. It is specified as a comma-delimited series of values--any value that would be acceptable on the Project Settings page of the Tracker web application may be used here. If an exact match to one of the built-in point scales, the project will use that point scale. If another comma-separated point-scale string is passed, it will be treated as a "custom" point scale. The built-in scales are "0,1,2,3", "0,1,2,4,8", and "0,1,2,3,5,8".
 
point_scale_is_custom boolean 
 —  True if the value of the point_scale string represents a custom, user-defined point scale rather than one of the ones built into Pivotal Tracker. This is important because of restrictions on moving stories from projects using a custom point scale into one using a standard point scale. Note that the set of built-in point scales is not considered part of the definition of an API version. Clients should be capable of processing any point_scale string that adheres to the format described above, and rely on this flag (rather than any explicit list that the client contains) to determine whether the project's point_scale is custom or standard. This field is read only.
 
bugs_and_chores_are_estimatable boolean 
 —  When true, Tracker will allow estimates to be set on Bug- and Chore-type stories. This is strongly not recommended. Please see the FAQ for more information.
 
automatic_planning boolean 
 —  When false, Tracker suspends the emergent planning of iterations based on the project's velocity, and allows users to manually control the set of unstarted stories included in the Current iteration. See the FAQ for more information.
 
enable_following boolean 
 —  When true, Tracker allows users to follow stories and epics, as well as use @mentions in comments. This field is read only.
 
enable_tasks boolean 
 —  When true, Tracker allows individual tasks to be created and managed within each story in the project.
 
start_date date 
 —  The first day that should be in an iteration of the project. If both this and "week_start_day" are supplied, they must be consistent. It is specified as a string in the format "YYYY-MM-DD" with "01" for January. If this is not supplied, it will remain blank (null), but "start_time" will have a default value based on the stories in the project.
 
time_zone time_zone 
 —  The "native" time zone for the project, independent of the time zone(s) from which members of the project view or modify it.
 
velocity_averaged_over int 
 —  The number of iterations that should be used when averaging the number of points of Done stories in order to compute the project's velocity.
 
shown_iterations_start_time datetime 
 —  The start time of the first iteration for which stories will be returned as part of the project, see 'number_of_done_iterations_to_show'. This field is read only. This field is excluded by default.
 
start_time datetime 
 —  The computed start time of the project, based on the other project attributes and the stories contained in the project. If they are provided, the value of start_time will be based on week_start_day and/or start_date. However, if the project contains stories with accepted_at dates before the time that would otherwise be computed, the value returned in start_time will be adjusted accordingly. This field is read only.
 
number_of_done_iterations_to_show int 
 —  There are areas within the Tracker UI and the API in which sets of stories automatically exclude the Done stories contained in older iterations. For example, in the web UI, the DONE panel doesn't necessarily show all Done stories by default, and provides a link to click to cause the full story set to be loaded/displayed. The value of this attribute is the maximum number of Done iterations that will be loaded/shown/included in these areas.
 
has_google_domain boolean 
 —  When true, the project has been associated with a Google Apps domain. Unless this is true, the /projects/{project_id}/google_attachments endpoint and the google_attachment resource cannot be used. This field is read only.
 
description string[140] 
 —  A description of the project's content. Entered through the web UI on the Project Settings page.
 
profile_content string[65535] 
 —  A long description of the project. This is displayed on the Project Overview page in the Tracker web UI.
 
enable_incoming_emails boolean 
 —  When true, the project will accept incoming email responses to Tracker notification emails and convert them to comments on the appropriate stories.

initial_velocity int 
 —  The number which should be used as the project's velocity when there are not enough recent iterations with Done stories for an actual velocity to be computed.
 
public boolean 
 —  When true, Tracker will allow any user on the web to view the content of the project. The project will not count toward the limits of a paid subscription, and may be included on Tracker's Public Projects listing page.
 
atom_enabled boolean 
 —  When true, Tracker allows people to subscribe to the Atom (RSS, XML) feed of project changes.
 
current_iteration_number int 
 —  Current iteration number for the project. This field is read only.
 
current_velocity int 
 —  Current velocity for the project. This field is read only. This field is excluded by default.
 
current_volatility float 
 —  Relative standard deviation of the points (adjusted for team strength and iteration length) completed over the number iterations used to compute velocity. This field is read only. This field is excluded by default.
 
account_id int 
 —  The ID number for the account which contains the project.
 
accounting_type enumerated string 
 —  One of the defined accounting types. This field is excluded by default.
Valid enumeration values: unbillable, billable, overhead
 
featured boolean 
 —  Whether or not the project will be included on Tracker's Featured Public Projects web page. This field is excluded by default.
 
story_ids List[int] 
 —  IDs of stories currently in the project. It is possible that not all stories in the 'accepted' state will be included in this list. Only those stories accepted since the begining of a particular done iteration will be returned. This is controlled by an entry on the project's Settings page in the Tracker web user interface, and the state of that entry is reflected in the number_of_done_iterations_to_show property of the project. This property contains a number of iterations. Tracker counts back this number of iterations prior to the present Current iteration, and will not include stories from Done iterations prior to this group. To access these stories, use the GET /projects/##/stories endpoint. This field is read only. This field is excluded by default. In API responses, this attribute may be story_ids or stories.
 
epic_ids List[int] 
 —  IDs of epics currently in the project. This field is read only. This field is excluded by default. In API responses, this attribute may be epic_ids or epics.
 
membership_ids List[int] 
 —  IDs of the exising memberships. This field is read only. This field is excluded by default. In API responses, this attribute may be membership_ids or memberships.
 
label_ids List[int] 
 —  IDs of labels currently in the project. This field is read only. This field is excluded by default. In API responses, this attribute may be label_ids or labels.
 
integration_ids List[int] 
 —  IDs of integrations currently configured for the project. Note that integration information must be retrieved by getting project information with integrations included as a nested resource; there is currently no independent RESTy endpoint for accessing integrations. This field is read only. This field is excluded by default. In API responses, this attribute may be integration_ids or integrations.
 
iteration_override_numbers List[int] 
 —  IDs of iteration overrides currently configured for the project. Note that iteration override information must be retrieved by getting project information with iteration overrides included as a nested resource; there is currently no independent RESTy endpoint for accessing iteration overrides, but there is one for iterations, which contains the same info plus additional dynamic fields related to emergent iteration calculation. This field is read only. This field is excluded by default. In API responses, this attribute may be iteration_override_numbers or iteration_override_numbers.
 
created_at datetime 
 —  Creation time. This field is read only.
 
updated_at datetime 
 —  Time of last update. This field is read only.
 
kind string 
 —  The type of this object: project. This field is read only.
 */
)

class HttpTool(headers:Header*) {
  val client = HttpClientBuilder.create().setDefaultHeaders(headers).build()
  
  def getJson[T](url:String)(implicit manifest:Manifest[T]):T = {
        val request = new HttpGet(url)
        val response = client.execute(request)
        val e = response.getEntity()
        val result = Jackson.jackson.readValue[T](e.getContent(), manifest.erasure.asInstanceOf[Class[T]])
        e.consumeContent()
        result
  }
  def putJson[T](url:String, data:T)(implicit manifest:Manifest[T]):Unit = {
        val request = new HttpPut(url)
        request.setEntity(new ByteArrayEntity(Jackson.jackson.writeValueAsBytes(data)))
        val response = client.execute(request)
        if(response.getStatusLine().getStatusCode()!=200) throw new Exception("Result: " + response.getStatusLine())
  }
}

class PivotalProjectTool(val authToken:String, val pivotalProjectId:String) {
  val baseUrl = s"https://www.pivotaltracker.com/services/v5/projects/${pivotalProjectId}"
  
  val t = new HttpTool(new BasicHeader("X-TrackerToken", authToken))
  
  def getStories() = t.getJson[Array[PT5Story]](s"${baseUrl}/stories")
  def getEpics() = t.getJson[Array[PT5Epic]](s"${baseUrl}/epics")
  def getProject() = t.getJson[PT5Project](baseUrl)
}

object PivotalSync {
  
    def main(args:Array[String]){
      
        val pivotalAPIKey = args(0)
        val pivotalProjectId = args(1)
        val teamStoryBaseUrl = args(2)
        val teamStoryId = args(3)
        val t = new PivotalProjectTool(pivotalAPIKey, pivotalProjectId)
        
        while(true){
          
            val stories = t.getStories()
            val epics = t.getEpics
            val project = t.getProject()
                    
                    
            val backlog = org.teamstory.datas.Backlog(
                    id=teamStoryId.toString,
                    name=project.name,
                    memo="work-in-progress",
                    projectedVelocity = None,
                    items= toItemList(stories, epics)
                    )
                            
            val teamStory = new HttpTool()
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