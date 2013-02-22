
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
  def main(args: Array[String]) {
    
    var data = Jerkson.parse[Backlog](getClass().getResourceAsStream("/sample-data.js"))
    
    HttpObjectsJettyHandler.launchServer(8080, 
        new HttpObject("/api/backlogs/{id}"){
            override def get(req:Request) = {
              OK(JerksonJson(data))
            }
            override def put(req:Request) = {
              data = Jerkson.parse[Backlog](readAsStream(req.representation()));
              println("New Data:\n" + data)
              get(req)
            }
        },
        new ClasspathResourceObject("/mockup", "/content/backlog-mockup.html", getClass()),
        new ClasspathResourceObject("/", "/content/backlog.html", getClass()),
        new ClasspathResourcesObject("/{resource*}", getClass(), "/content")
    ); 
  }
}