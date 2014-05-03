package com.cj.etherlog.http

import org.httpobjects._
import org.httpobjects.DSL._
import com.cj.etherlog.data.Data
import com.cj.etherlog.Jackson
import com.cj.etherlog.api.BacklogStatusPatch
import com.cj.etherlog.Etherlog
import com.cj.etherlog.Jackson._
import com.cj.etherlog.Service
import HttpUtils._

class StatsLogResource (data:Data, service:Service) extends HttpObject("/api/backlogs/{id}/statsLog"){
    override def get(req:Request) = {
      val id = req.path().valueFor("id")
      val stats = buildStatsLogFromQueryString(id, req, data);
      
      OK(JerksonJson(stats))
    }
}