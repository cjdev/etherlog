package org.teamstory.http

import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.datas.Data
import org.teamstory.Jackson
import org.teamstory.api.BacklogStatusPatch
import org.teamstory.TeamStory
import org.teamstory.Jackson._
import org.teamstory.Service
import HttpUtils._
import org.teamstory.Clock

class StatsLogResource (data:Data, service:Service, clock:Clock) extends HttpObject("/api/backlogs/{id}/statsLog"){
    override def get(req:Request) = {
      val id = req.path().valueFor("id")
      val stats = buildStatsLogFromQueryString(id, req, data, clock);
      
      OK(JacksonJson(stats))
    }
}