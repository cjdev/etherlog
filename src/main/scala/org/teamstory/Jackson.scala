package org.teamstory

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.io.{InputStream, OutputStream}
import org.httpobjects.Representation
import org.teamstory.http.HttpUtils

object Jackson {

  val jackson:ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper
  }
  
    
  def parseJson[T](is:InputStream)(implicit manifest:Manifest[T]):T = {
    jackson.readValue[T](is, manifest.erasure.asInstanceOf[Class[T]])
  }
  
  def parse[T](r:Representation)(implicit manifest:Manifest[T]) = Jackson.parseJson[T](HttpUtils.readAsStream(r));
  
  def JacksonJson(o:AnyRef) = {
    new Representation(){
      override def contentType = "application/json"
      override def write(out:OutputStream){
        Jackson.jackson.writeValue(out, o)
      }
    }
  }
}