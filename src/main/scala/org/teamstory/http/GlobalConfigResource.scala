package org.teamstory.http

import org.teamstory.datas.Data
import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.api.GlobalConfig
import org.teamstory.Jackson

class GlobalConfigResource(data:Data) extends HttpObject("/api/config"){
  override def get(r:Request) = {
    OK(Jackson.JacksonJson(data.getGlobalConfig().withoutSecrets()))
  }
  override def put(r:Request) = {
    val newConfig = Jackson.parse[GlobalConfig](r.representation())
    
    data.setGlobalConfig(newConfig);
    
    get(r)
  }
}