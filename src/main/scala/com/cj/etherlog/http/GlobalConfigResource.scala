package com.cj.etherlog

import com.cj.etherlog.datas.Data
import org.httpobjects._
import org.httpobjects.DSL._
import com.cj.etherlog.api.GlobalConfig

class GlobalConfigResource(data:Data) extends HttpObject("/api/config"){
  override def get(r:Request) = OK(Jackson.JerksonJson(data.getGlobalConfig()))
  override def put(r:Request) = {
    val newConfig = Jackson.parse[GlobalConfig](r.representation())
    
    data.setGlobalConfig(newConfig);
    
    get(r)
  }
}