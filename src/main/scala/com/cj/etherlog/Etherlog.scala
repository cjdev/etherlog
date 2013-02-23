
package com.cj.etherlog

import org.httpobjects.jetty.HttpObjectsJettyHandler
import org.httpobjects.HttpObject
import org.httpobjects.DSL._
import org.httpobjects.jackson.JacksonDSL._
import org.httpobjects.Request
import org.httpobjects.util.ClasspathResourcesObject
import org.httpobjects.util.ClasspathResourceObject
import java.io.InputStream
import java.io.InputStreamReader
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import org.codehaus.jackson.map.ObjectMapper
import com.codahale.jerkson.{Json => Jerkson}
import org.httpobjects.Representation
import java.io.OutputStream
import java.io.{File => Path}
import java.util.UUID
import scala.collection.mutable.ListBuffer

object Etherlog {
  def readAsStream(r:Representation) = {
    val bytes = new ByteArrayOutputStream()
    r.write(bytes);
    bytes.close()
    new ByteArrayInputStream(bytes.toByteArray())
  }
  def asString(is:InputStream):String = {
    val reader = new InputStreamReader(is)
    val buffer = new Array[Char](33)
    val text = new StringBuilder()
    
    var numRead = reader.read(buffer);
    while(numRead>=0){
       text.append(buffer, 0, numRead)
       numRead = reader.read(buffer);
    }
    text.toString()
  }
  
  def JerksonJson(o:AnyRef) = {
    new Representation(){
      override def contentType = "application/json"
      override def write(out:OutputStream){
        Jerkson.generate(o, out)
      }
    }
  }
  
  class Database[T](basePath:Path){
    basePath.mkdirs();
    
    def put(id:String, data:T):Unit  = this.synchronized{
      Jerkson.generate(data, pathFor(id))
    }
    
    def get(id:String)(implicit manifest:Manifest[T]):T = this.synchronized {
        Jerkson.parse[T](pathFor(id))
    }
    
    def contains(id:String) = pathFor(id).exists()
    
    private def pathFor(id:String) = new Path(basePath, id);
  }
  
  def main(args: Array[String]) {
    
    val dataPath = new Path("data");
    
    val backlogs = new Database[BacklogStatus](new Path(dataPath, "backlogs"))
    val versions = new Database[BacklogVersion](new Path(dataPath, "versions"))
    
    if(!backlogs.contains("23")){
      val template = Jerkson.parse[Backlog](getClass().getResourceAsStream("/sample-data.js"))
      val initialBacklog = new Backlog(
                              id="23", 
                              name= template.name,
                              memo="initial sample version",
                              items = template.items)
      
      val initialVersion = new BacklogVersion(
                                  id = UUID.randomUUID().toString(),
                                  when = System.currentTimeMillis(),
                                  isPublished = true,
                                  previousVersion = null,
                                  backlog = initialBacklog
                              )
      
        backlogs.put(
                id="23", 
                data = new BacklogStatus(id="23", latestVersion = initialVersion.id))
        versions.put(initialVersion.id, initialVersion)
    }
    
    HttpObjectsJettyHandler.launchServer(8080, 
        
        new HttpObject("/api/backlogs/{id}/history"){
            override def get(req:Request) = {
              val id = req.pathVars().valueFor("id")
              val backlog = backlogs.get(id);
              val results = new ListBuffer[String]()
              
              var nextVersionId = backlog.latestVersion
              while(nextVersionId!=null){
                val version = versions.get(nextVersionId)
                results += version.id
                nextVersionId = version.previousVersion
              }
              
              OK(JerksonJson(results))
            }
            
        },
        new HttpObject("/api/backlogs/{id}"){
            override def get(req:Request) = {
              val id = req.pathVars().valueFor("id")
              val backlog = backlogs.get(id)
              val currentVersion = versions.get(backlog.latestVersion);
              OK(JerksonJson(currentVersion.backlog))
            }
            override def put(req:Request) = {
              val id = req.pathVars().valueFor("id")
              val newBacklog = Jerkson.parse[Backlog](readAsStream(req.representation()));
              val backlog = backlogs.get(id);
              val updatedBacklog = new BacklogStatus(
                                          backlog.id, 
                                          latestVersion = UUID.randomUUID().toString())
              
              val newVersion = new BacklogVersion(
                                  id = UUID.randomUUID().toString(),
                                  when = System.currentTimeMillis(),
                                  isPublished = false,
                                  previousVersion = backlog.latestVersion,
                                  backlog = newBacklog
                              )
              
              versions.put(updatedBacklog.latestVersion, newVersion);
              backlogs.put(id, updatedBacklog)
              println("New Data:\n" + newVersion)
              get(req)
            }
        },
        new ClasspathResourceObject("/mockup", "/content/backlog-mockup.html", getClass()),
        new ClasspathResourceObject("/", "/content/backlog.html", getClass()),
        new ClasspathResourcesObject("/{resource*}", getClass(), "/content")
    ); 
  }
}