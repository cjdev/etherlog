package org.teamstory.authenticate

import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.Jackson
import org.teamstory.datas.Data

class SessionResource (data:Data) extends HttpObject("/api/sessions/{id}"){
    override def get(r:Request)={
      val id = r.path().valueFor("id")
      data.sessions.get(id) match {
        case null => NOT_FOUND()
        case session => {
          val user = data.users.get(session.email)
          OK(Jackson.JerksonJson(user))
        }
      }
    }
}