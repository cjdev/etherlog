package org.teamstory.http

import org.httpobjects.test.MockRequest
import org.httpobjects.{Response, Request}
import org.teamstory.datas.{DataStub, DatabaseTrait, Data}
import org.teamstory.api.TeamDto
import org.scalatest.FunSuite

class TeamResourceTest extends FunSuite {

    test("happy") {
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
                def filter(fn:(String, TeamDto)=>Boolean)(implicit manifest:Manifest[TeamDto]) = Stream()
            }
        
        }
        val teamResource: TeamResource = new TeamResource(data, null)
        val request: Request = new MockRequest(teamResource, "/api/team/8")
        val response: Response = teamResource.get(request).get

        assert(200 === response.code().value())
    }
}
