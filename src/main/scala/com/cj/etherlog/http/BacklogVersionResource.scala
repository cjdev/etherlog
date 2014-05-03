package com.cj.etherlog.http
import org.httpobjects._
import org.httpobjects.DSL._
import com.cj.etherlog.data.Data
import com.cj.etherlog.Jackson
import com.cj.etherlog.api.BacklogStatusPatch
import com.cj.etherlog.Etherlog
import com.cj.etherlog.Jackson._
import scala.collection.mutable.ListBuffer
import com.cj.etherlog.data.Data
import com.cj.etherlog.Service
import com.cj.etherlog.api.BacklogListEntry
import com.cj.etherlog.Backlog
import com.cj.etherlog.api.HistoryItem

class BacklogVersionResource(data:Data) extends HttpObject("/api/backlogs/{id}/history/{version}"){
    override def get(req:Request) = {
      val id = req.path().valueFor("id")
      val versionId = req.path().valueFor("version")
      val results = new ListBuffer[HistoryItem]()
      
      if(data.versions.contains(versionId)){
        val version = data.versions.get(versionId);
          
        OK(JerksonJson(version.backlog))
      }else{
        NOT_FOUND(Text("No such version"))
      }
    }
    
}