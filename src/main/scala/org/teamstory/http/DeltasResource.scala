package org.teamstory.http

import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.Jackson._
import org.teamstory.datas.Data
import org.teamstory.Clock
import org.teamstory.api.Delta

class DeltasResource(data: Data, clock: Clock) extends HttpObject("/api/backlogs/{id}/deltas") {

  override def get(req: Request) = {
    val id = req.path().valueFor("id")

    def toLongOr(s: String, default: Long) = if (s == null) default else s.toLong

    val from = toLongOr(req.query().valueFor("from"), 0)
    val to = toLongOr(req.query().valueFor("to"), clock.now.getMillis())
    val backlog = data.backlogs.get(id)

    val changes = data.filterBacklogHistory(id, {
      version =>
        (version.backlog.memo != "work-in-progress") &&
                (version.when > from) &&
                (version.when <= to)
    }).reverse

    val deltas: Seq[Delta] = changes.zipWithIndex.flatMap {
      item =>
        val (version, idx) = item;
        if (idx > 0) {
          val previous = changes(idx - 1)
          Some(version.delta(previous))
        } else {
          None
        }
    }

    OK(JacksonJson(deltas))
  }
}