package org.teamstory.pivotal

import org.apache.http.impl.client._
import org.apache.http._
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.client.methods._
import scala.collection.JavaConversions._
import org.teamstory.Jackson

class JacksonHttpTool(headers:Header*) {
  val client = HttpClientBuilder.create().setDefaultHeaders(headers).build()
  
  def getJson[T](url:String)(implicit manifest:Manifest[T]):T = {
        println(s"[GET] $url")
        val request = new HttpGet(url)
        val response = client.execute(request)
        val e = response.getEntity()
        val result = Jackson.jackson.readValue[T](e.getContent(), manifest.erasure.asInstanceOf[Class[T]])
        e.consumeContent()
        result
  }
  def putJson[T](url:String, data:T)(implicit manifest:Manifest[T]):Unit = {
        println(s"[PUT] $url")
        val request = new HttpPut(url)
        request.setEntity(new ByteArrayEntity(Jackson.jackson.writeValueAsBytes(data)))
        val response = client.execute(request)
        if(response.getStatusLine().getStatusCode()!=200) throw new Exception("Result: " + response.getStatusLine())
  }
}