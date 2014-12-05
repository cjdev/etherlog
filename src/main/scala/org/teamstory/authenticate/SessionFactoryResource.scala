package org.teamstory.authenticate

import org.httpobjects._
import org.httpobjects.DSL._
import java.util.UUID
import org.teamstory.datas.Data
import org.teamstory.Jackson
import org.httpobjects.header.response.SetCookieField
import org.teamstory.Service

class SessionFactoryResource(val datas:Data, val authMechanisms:Seq[AuthMechanism]) extends HttpObject("/api/sessions"){
    override def post(r:Request)= {
        val request = Jackson.parse[AuthRequest](r.representation())
        
        
        def authenticationResults(authMechanisms: Seq[AuthMechanism]): Stream[Option[AuthDetails]] = {
          if (authMechanisms.isEmpty) {
            Stream.empty
          } else {
            val authMechanism = authMechanisms.head
            val value = authMechanism.authenticateEmail(request.email, request.password)
            value #:: authenticationResults(authMechanisms.tail)
          }
        }
       
        authenticationResults(authMechanisms).find(_.isDefined) match {
          case Some(userInfo) => {
            val user = getUserWithCreateIfNeeded(request.email)
            val session = Session(id=UUID.randomUUID().toString, email = request.email)
            datas.sessions.put(session.id, session)
            new Response(
                ResponseCode.CREATED, 
                Jackson.JerksonJson(session), 
                Location("/api/sessions/" + session.id))
          }
          case None => UNAUTHORIZED()
        }
    }
    
    private def getUserWithCreateIfNeeded(email:String) = {
      datas.users.getOption(email) match {
        case None => {
          val user = User(email);
          datas.users.put(email, user)
          user
        }
        case Some(user) => user
      }
    }
}