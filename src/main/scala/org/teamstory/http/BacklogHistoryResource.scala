package org.teamstory.http

import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.Util
import org.teamstory.datas.Data
import org.teamstory.TeamStory
import org.teamstory.Jackson._
import HttpUtils._

class BacklogHistoryResource(val data:Data) extends HttpObject("/api/backlogs/{id}/history"){
    override def get(req:Request) = {
        val id = req.path().valueFor("id")
        val showLatestEvenIfWipParam = req.query().valueFor(showLatestEvenIfWipParamName)
        
        val showLatestEvenIfWip = Util.parseBoolean(showLatestEvenIfWipParam).getOrElse(false)
        
        val fullHistory = data.getHistory(id)
        
        val results = fullHistory.filter{item=>
            val isLast = fullHistory.head eq item
            (showLatestEvenIfWip && isLast) || item.memo != "work-in-progress"
        }
        OK(JacksonJson(results))
    }

}