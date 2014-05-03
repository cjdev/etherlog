package com.cj.etherlog

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
import com.cj.etherlog.api._
import com.cj.etherlog.chart._
import org.joda.time.Months
import org.joda.time.Weeks
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.HttpClient
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat
import org.joda.time.YearMonthDay
import com.cj.etherlog.data.BacklogVersion
import com.cj.etherlog.http.BacklogHistoryResource
import com.cj.etherlog.data.Data
import com.cj.etherlog.Jackson._
import com.cj.etherlog.http.BacklogStatusResource
import com.cj.etherlog.http.ChartResource
import com.cj.etherlog.http.BacklogsResource
import com.cj.etherlog.http.StatsLogResource
import com.cj.etherlog.http.BacklogVersionResource
import com.cj.etherlog.http.DeltaResource
import com.cj.etherlog.http.DeltasResource
import com.cj.etherlog.http.BacklogResource
import com.cj.etherlog.http.ErrorsResource

object Etherlog {
  
  def main(args: Array[String]) {
    val data = new Data(new Path("data"))
    val service = new Service(data)
    
    class ChartStylesheet(parentPath:String) extends HttpObject(parentPath + "/mystyle.css"){
        override def get(req:Request) = {
          OK(FromClasspath("text/css", "/content/mystyle.css", getClass))
        }
    }
     
    val port = 43180
    
    launchServer(port, 
        "/api/backlogs" -> new BacklogsResource(data=data, service=service),
        "/api/backlogs/{id}/mystyle.css" -> new ChartStylesheet("/api/backlogs/{id}"),
        "/backlog/mystyle.css" -> new ChartStylesheet("/backlog"),
        "/api/backlogs/{id}/chart" -> new ChartResource(data=data, service=service),
        "/api/backlogs/{id}/history" -> new BacklogHistoryResource(data=data),
        "/api/backlogs/{id}/status" -> new BacklogStatusResource(data),
        "/api/backlogs/{id}/statsLog" -> new StatsLogResource(data=data, service=service),
        "/api/backlogs/{id}/history/{version}" -> new BacklogVersionResource(data=data),
        "/api/backlogs/{id}" -> new BacklogResource(data, service=service),
        "/api/errors" -> new ErrorsResource(data),
        "/api/backlogs/{id}/deltas" -> new DeltasResource(data),
        "/api/backlogs/{id}/deltas/{rangeSpec}" -> new DeltaResource(data),
        "/overview" -> new ClasspathResourceObject("/overview", "/content/birdview.html", getClass()),
        "/" -> new ClasspathResourceObject("/", "/content/index.html", getClass()),
        "/backlog/{backlogId}" -> new ClasspathResourceObject("/backlog/{backlogId}", "/content/backlog.html", getClass()),
        "/{resource*}" -> new ClasspathResourcesObject("/{resource*}", getClass(), "/content")
    );
    
    println("etherlog is alive and listening on port " + port);
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
}