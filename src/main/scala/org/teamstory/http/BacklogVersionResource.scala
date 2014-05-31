package org.teamstory.http
import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.datas.Data
import org.teamstory.Jackson
import org.teamstory.api.BacklogStatusPatch
import org.teamstory.TeamStory
import org.teamstory.Jackson._
import scala.collection.mutable.ListBuffer
import org.teamstory.Service
import org.teamstory.api.BacklogListEntry
import org.teamstory.datas.Backlog
import org.teamstory.api.HistoryItem

class BacklogVersionResource(data:Data) extends HttpObject("/api/backlogs/{id}/history/{version}"){
    override def get(req:Request) = {
      val id = req.path().valueFor("id")
      val versionId = req.path().valueFor("version")
      val results = new ListBuffer[HistoryItem]()
      
      if(data.versions.contains(versionId)){
        val version = data.versions.get(versionId);
        OK(JerksonJson(version.backlog.toDto(versionId)))
      }else{
        NOT_FOUND(Text("No such version"))
      }
    }
    
}