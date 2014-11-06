package org.teamstory

import org.httpobjects.jetty.HttpObjectsJettyHandler
import org.httpobjects.HttpObject
import org.httpobjects.DSL._
import org.httpobjects.Request
import org.httpobjects.util.ClasspathResourcesObject
import org.httpobjects.util.ClasspathResourceObject
import java.io.InputStream
import java.io.InputStreamReader
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import org.httpobjects.Representation
import java.io.OutputStream
import java.io.{File => Path}
import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.StringBuilder
import java.io.FileInputStream
import org.joda.time.DateMidnight
import org.joda.time.Instant
import org.teamstory.api._
import org.teamstory.chart._
import org.joda.time.Months
import org.joda.time.Weeks
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.HttpClient
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat
import org.joda.time.YearMonthDay
import org.teamstory.datas.{DataImpl, BacklogVersion, Data}
import org.teamstory.http.BacklogHistoryResource
import org.teamstory.Jackson._
import org.teamstory.http.BacklogStatusResource
import org.teamstory.http.DefaultChartResource
import org.teamstory.http.BacklogsResource
import org.teamstory.http.StatsLogResource
import org.teamstory.http.BacklogVersionResource
import org.teamstory.http.DeltaResource
import org.teamstory.http.DeltasResource
import org.teamstory.http.BacklogResource
import org.teamstory.http.ErrorsResource
import org.teamstory.http.TimeTravelResource
import org.teamstory.http.IterationBarChartResource
import org.teamstory.http.TeamResource
import org.teamstory.http.TeamsResource
import org.teamstory.http.TeamIterationResource
import org.teamstory.http.TeamIterationStatsResource
import org.teamstory.http.GlobalConfigResource
import org.teamstory.pivotal.PivotalSync
import org.teamstory.pivotal.PivotalTrackerV5ApiStub
import org.teamstory.pivotal.PivotalSyncThread

object TeamStory {
  
  def timeTravelModeIsActivated(args:Array[String]) = args.size >0 && args(0) == "enableTimeTravel"

  def main(args: Array[String]) {
    val data = new DataImpl(new Path("data"))

    val clock = new FastForwardableClock(
                        configDb=data,
                        enableTimeTravel = timeTravelModeIsActivated(args))

    val service = new Service(data, clock)

    val port = 43180

    launchServer(port,
        "/api/backlogs/{id}/iteration-stats" -> new TeamIterationStatsResource(data=data, clock=clock),
        "/api/team/{id}/iteration" -> new TeamIterationResource(data=data, clock=clock),
        "/api/team" -> new TeamsResource(data=data),
        "/api/team/{id}" -> new TeamResource(data=data, clock=clock),
        "/api/config" -> new GlobalConfigResource(data=data),
        "/api/backlogs/{id}/chart/iteration-bars" -> new IterationBarChartResource(data=data, clock=clock),
        "/api/clock" -> new TimeTravelResource(clock=clock),
        "/api/backlogs" -> new BacklogsResource(data=data, service=service),
        "/api/backlogs/{id}/css/mystyle.css" -> new ChartStylesheet("/api/backlogs/{id}"),
        "/backlog/css/mystyle.css" -> new ChartStylesheet("/backlog"),
        "/api/backlogs/{id}/chart/default" -> new DefaultChartResource(data=data, service=service, clock=clock),
        "/api/backlogs/{id}/history" -> new BacklogHistoryResource(data=data),
        "/api/backlogs/{id}/status" -> new BacklogStatusResource(data, clock=clock),
        "/api/backlogs/{id}/statsLog" -> new StatsLogResource(data=data, service=service, clock=clock),
        "/api/backlogs/{id}/history/{version}" -> new BacklogVersionResource(data=data),
        "/api/backlogs/{id}" -> new BacklogResource(data, service=service, clock=clock),
        "/api/errors" -> new ErrorsResource(data),
        "/api/backlogs/{id}/deltas" -> new DeltasResource(data, clock=clock),
        "/api/backlogs/{id}/deltas/{rangeSpec}" -> new DeltaResource(data, clock=clock),
        "/overview" -> new ClasspathResourceObject("/overview", "/content/birdview.html", getClass()),
        "/timemachine" -> new ClasspathResourceObject("/timemachine", "/content/timemachine.html", getClass()),
        "/" -> new ClasspathResourceObject("/", "/content/index.html", getClass()),
        "/backlog/{backlogId}" -> new ClasspathResourceObject("/backlog/{backlogId}", "/content/backlog.html", getClass()),
        "/team/{teamName}" -> new ClasspathResourceObject("/team/{teamName}", "/content/team.html", getClass()),
        "/team/{teamName}/iterations/{iterationEndDate}" -> new ClasspathResourceObject("/team/{teamName}/iterations/{iterationEndDate}", "/content/iteration.html", getClass()),
        "/{resource*}" -> new ClasspathResourcesObject("/{resource*}", getClass(), "/content")
    )

    println("etherlog is alive and listening on port " + port);
    
    new PivotalSyncThread(data, service).start()
  }

  
  
  def launchServer(port:Int, resources:(String, HttpObject)*) = {
    val badMappings = resources.filter{entry=>
      val (pathMapping, r) = entry
      pathMapping!=r.pattern().raw()
    }

    if(!badMappings.isEmpty){
      throw new Exception("Ummm, hey, these mappings don't match up:" + badMappings.map{m=>(m._1, m._2.pattern().raw)}.mkString("\n"))
    }

    HttpObjectsJettyHandler.launchServer(port, resources.map(_._2):_*)
  }

  class ChartStylesheet(parentPath:String) extends HttpObject(parentPath + "/css/mystyle.css"){
    override def get(req:Request) = {
      OK(FromClasspath("text/css", "/content/css/mystyle.css", getClass))
    }
  }
}
