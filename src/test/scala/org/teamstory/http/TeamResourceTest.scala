package org.teamstory.http

import org.junit.Test
import org.httpobjects.test.MockRequest
import org.httpobjects.{Response, Request}
import org.junit.Assert._
import org.teamstory.datas.{DataStub, DatabaseTrait, Data}
import org.teamstory.api.TeamDto

class TeamResourceTest {

    @Test
    def happy() {
        val data:Data  = new DataStub() {
            override val teams: DatabaseTrait[TeamDto] = new DatabaseTrait[TeamDto]() {
                override def get(id: String)(implicit manifest: Manifest[TeamDto]): TeamDto = {
                    TeamDto("ralph", "suzy", Seq())
                }
                def put(id:String, data:TeamDto):Unit = ()
                def contains(id:String): Boolean = false
                def toStream(implicit manifest:Manifest[TeamDto]):Stream[TeamDto] = null
                def map[R](fn:(String, TeamDto)=>R)(implicit manifest:Manifest[TeamDto]):Seq[R] = null
                def scan(fn:(String, TeamDto)=>Unit)(implicit manifest:Manifest[TeamDto]) = ()
            }
        
        };
        val teamResource: TeamResource = new TeamResource(data, null)
        val request: Request = new MockRequest(teamResource, "/api/team/8")
        val response: Response = teamResource.get(request)

        assertEquals(200, response.code().value())
    }
}
