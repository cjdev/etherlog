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
import scala.collection.JavaConversions._
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
import org.teamstory.authenticate._
import org.httpobjects.Response
import org.teamstory.http.GenericWrapper
import org.httpobjects.ResponseCode

object TeamStory {
  
  def timeTravelModeIsActivated(args:Array[String]) = args.size >0 && args(0) == "enableTimeTravel"
    
  def loadAuthMechanism(config:GlobalConfig) = config.maybeLdapConfig  match {
    case Some(ldapConfig) => {
      new LdapTool(
            url=ldapConfig.ldapUrl, 
            ldapUser = ldapConfig.ldapUser, 
            ldapPassword = ldapConfig.ldapPassword)
    }
    case None => new AuthMechanism(){
      def authenticateEmail(email:String, password:String) = Option(AuthDetailsPlaceholder)
          def emailExists(email:String) = true
    }
  }
  
  
  
  def main(args: Array[String]) {
    val data = new DataImpl(new Path("data"))
    val authMechanism = loadAuthMechanism(data.getGlobalConfig());
    
    val clock = new FastForwardableClock(
                        configDb=data,
                        enableTimeTravel = timeTravelModeIsActivated(args))

    val service = new Service(data, clock)

    val port = 43180

    def noAnonymous(w:HttpObject) = requireSession(w, service)
    
    launchServer(port,
        /** API */
        "/api/clock" -> noAnonymous(new TimeTravelResource(clock=clock)),
        "/api/config" -> noAnonymous(new GlobalConfigResource(data=data)),
        "/api/backlogs" -> noAnonymous(new BacklogsResource(data=data, service=service)),
        "/api/backlogs/{id}/chart/default" -> noAnonymous(new DefaultChartResource(data=data, service=service, clock=clock)),
        "/api/backlogs/{id}/chart/iteration-bars" -> noAnonymous(new IterationBarChartResource(data=data, clock=clock)),
        "/api/backlogs/{id}/css/mystyle.css" -> noAnonymous(new ChartStylesheet("/api/backlogs/{id}")),
        "/api/backlogs/{id}/deltas" -> noAnonymous(new DeltasResource(data, clock=clock)),
        "/api/backlogs/{id}/deltas/{rangeSpec}" -> noAnonymous(new DeltaResource(data, clock=clock)),
        "/api/backlogs/{id}/history" -> noAnonymous(new BacklogHistoryResource(data=data)),
        "/api/backlogs/{id}/history/{version}" -> noAnonymous(new BacklogVersionResource(data=data)),
        "/api/backlogs/{id}/iteration-stats" -> noAnonymous(new TeamIterationStatsResource(data=data, clock=clock)),
        "/api/backlogs/{id}/statsLog" -> noAnonymous(new StatsLogResource(data=data, service=service, clock=clock)),
        "/api/backlogs/{id}/status" -> noAnonymous(new BacklogStatusResource(data, clock=clock)),
        "/api/backlogs/{id}" -> noAnonymous(new BacklogResource(data, service=service, clock=clock)),
        "/api/errors" -> noAnonymous(new ErrorsResource(data)),
        "/api/sessions" -> new SessionFactoryResource(datas=data, authMechanism=authMechanism),
        "/api/sessions/{id}" -> noAnonymous(new SessionResource(data=data)),
        "/api/team" -> noAnonymous(new TeamsResource(data=data)),
        "/api/team/{id}/iteration" -> noAnonymous(new TeamIterationResource(data=data, clock=clock)),
        "/api/team/{id}" -> noAnonymous(new TeamResource(data=data, clock=clock)),
        
        /** UI */
        "/" -> requireLogin(new ClasspathResourceObject("/", "/content/index.html", getClass()), service),
        "/backlog/css/mystyle.css" -> new ChartStylesheet("/backlog"),
        "/backlog/{backlogId}" -> requireLogin(new ClasspathResourceObject("/backlog/{backlogId}", "/content/backlog.html", getClass()), service),
        "/login/{path*}" -> new ClasspathResourceObject("/login/{path*}", "/content/login.html", getClass()),
        "/overview" -> requireLogin(new ClasspathResourceObject("/overview", "/content/birdview.html", getClass()), service),
        "/team/{teamName}" -> requireLogin(new ClasspathResourceObject("/team/{teamName}", "/content/team.html", getClass()), service),
        "/team/{teamName}/iterations/{iterationEndDate}" -> requireLogin(new ClasspathResourceObject("/team/{teamName}/iterations/{iterationEndDate}", "/content/iteration.html", getClass()), service),
        "/timemachine" -> new ClasspathResourceObject("/timemachine", "/content/timemachine.html", getClass()),
        "/{resource*}" -> new ClasspathResourcesObject("/{resource*}", getClass(), "/content")
    )

    println("etherlog is alive and listening on port " + port);
    
    new PivotalSyncThread(data, service, clock).start()
  }

  def requireSession(w:HttpObject, service:Service):HttpObject = {
    new GenericWrapper(w, decorator={(method, request, fn)=>
      service.withAuthorizationRequired(request){user=>
        fn(request)
      }
    });
  }
  
  def requireLogin(w:HttpObject, service:Service):HttpObject = {
    new GenericWrapper(w, decorator={(method, request, fn)=>
      service.getAuthenticatedUser(request) match {
        case None => new Response(
                            ResponseCode.TEMPORARY_REDIRECT, 
                            Text("Unauthorized"), 
                            Location("/login" + request.path().toString()))
        case Some(user) => {
          fn(request)
        }
      }
    });
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
