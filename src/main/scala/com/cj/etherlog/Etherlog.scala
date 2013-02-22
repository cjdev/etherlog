
package com.cj.etherlog

import org.httpobjects.jetty.HttpObjectsJettyHandler
import org.httpobjects.HttpObject
import org.httpobjects.DSL._
import org.httpobjects.Request
import org.httpobjects.util.ClasspathResourcesObject
import org.httpobjects.util.ClasspathResourceObject

object Etherlog {
  def main(args: Array[String]) {
    HttpObjectsJettyHandler.launchServer(8080, 
        new HttpObject("/api/backlogs/{id}"){
            override def get(req:Request) = {
              OK(Bytes("application/json", getClass().getResourceAsStream("/sample-data.js")))
            }
        },
        new ClasspathResourceObject("/mockup", "/content/backlog-mockup.html", getClass()),
        new ClasspathResourceObject("/", "/content/backlog.html", getClass()),
        new ClasspathResourcesObject("/{resource*}", getClass(), "/content")
    ); 
  }
}