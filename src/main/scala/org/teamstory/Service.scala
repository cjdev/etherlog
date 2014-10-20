package org.teamstory

import org.teamstory.datas.{Backlog, BacklogVersion, Data, BacklogStatus}
import org.httpobjects.Request
import java.util.UUID
import scala.collection.mutable.ListBuffer
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.HttpClient

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
                isPublished= false,
                previousVersion = null)

        data.versions.put(initialVersion.id, initialVersion)

        val status = BacklogStatus(
                id=backlogId,
                latestVersion = initialVersion.id,
                whenArchived=None)

        data.backlogs.put(backlogId, status)
        status.id
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
