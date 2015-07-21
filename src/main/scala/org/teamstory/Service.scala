package org.teamstory

import org.teamstory.authenticate.{User}
import org.teamstory.datas.{Backlog, BacklogVersion, Data, BacklogStatus}
import org.httpobjects.Request
import java.util.UUID
import scala.collection.mutable.ListBuffer
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.HttpClient
import org.joda.time.Instant
import org.httpobjects.{HttpObject, Request, Response, Eventual}
import org.httpobjects.DSL.{UNAUTHORIZED}
import scala.collection.JavaConversions.iterableAsScalaIterable

class Service (data:Data, clock:Clock) {

    def createBacklog(newBacklogState:Backlog) = {

        val backlogId = this.synchronized{
            var candidate = 1
                    while(data.backlogs.contains(candidate.toString())){
                        candidate += 1;
                    }
            candidate.toString()
        }

        val initialBacklog = Backlog(
                id=backlogId,
                name=newBacklogState.name,
                memo=newBacklogState.memo,
                items = newBacklogState.items)

        val initialVersion = BacklogVersion(
                id=UUID.randomUUID().toString(),
                when = clock.now().getMillis(),
                backlog = initialBacklog,
                previousVersion = null)

        data.versions.put(initialVersion.id, initialVersion)

        val status = BacklogStatus(
                id=backlogId,
                latestVersion = initialVersion.id,
                whenArchived=None,
                pivotalTrackerLink = None)

        data.backlogs.put(backlogId, status)
        status.id
    }
    def getAuthenticatedUser[T](req:Request):Option[User] = {
      val cookies = req.header().cookies().filter(_.name == "session")
      println("cookies " + cookies)
      val maybeSessionCookie = cookies.headOption
      
      val maybeUser = maybeSessionCookie match {
        case None => None
        case Some(sessionCookie) => {
          val value = sessionCookie.value
          if(!value.isEmpty()){
              data.sessions.getOption(sessionCookie.value) match {
                  case None => None
                  case Some(session) => {
                      data.users.getOption(session.email)
                  }
              }
          }else{
            None
          }
        }
      }
      maybeUser
    }
    
    def withAuthorizationRequired[T](req:Request)(fn:(User)=>Eventual[Response]):Eventual[Response] = {
      getAuthenticatedUser(req) match {
        case None => UNAUTHORIZED()
        case Some(user) => {
          fn(user)
        }
      }
    }
    
    def saveBacklogUpdate(update:Backlog) {
      val id = update.id;
      val backlog = data.backlogs.get(id);
      val newVersion = new BacklogVersion(
                  id = UUID.randomUUID().toString(),
                  when = clock.now.getMillis,
                  previousVersion = backlog.latestVersion,
                  backlog = update)
      
      println("the time is now " + new Instant(newVersion.when))
      
      val updatedBacklog = backlog.copy(latestVersion = newVersion.id)
      data.versions.put(updatedBacklog.latestVersion, newVersion);
      data.backlogs.put(id, updatedBacklog)
      
      notifySubscribers(backlog)
    }

    def notifySubscribers(backlog: BacklogStatus) {
      SubscriberConfiguration.getSubscriberUrls("subscribers.properties") match {
        case Some(subscribers)=> {
            subscribers.foreach(url => {
              val subscriberUrl = url + backlog.id
              val postMethod = new PostMethod(subscriberUrl)
              val client = new HttpClient()
              val response = client.executeMethod(postMethod)

              if (response != 200) {
                println("NO RESPONSE FROM BOARD")
              } else {
                println("Board notified id " + backlog.id)
              }
            })
        }
        case None=> // nuthin to do here
      }

    }
}
