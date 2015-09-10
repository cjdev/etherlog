package org.teamstory.datas

import org.scalatest.FunSuite
import org.teamstory.api._
import java.util.UUID

import scala.util.{Success, Failure, Try}

class BacklogTest extends FunSuite {


  test("doesn't allow for null entries"){
    // given
    val items = Seq(
      aStory("do push-ups", Seq(aTeamEstimate(1))).copy(isComplete=Some(true)),
      null,
      aGoal(name = "Be Super Fit"))

    // when
    val maybeBacklog = Try(aBackLog(
      items = items))

    // then
    assertFailure(maybeBacklog,  new java.lang.IllegalStateException("backlogs cannot have null items"))
  }

  private def assertFailure(t:Try[_], template:Throwable): Unit ={

    def stringified(t:Throwable) = t.getClass.getName + ":" + t.getMessage

    assert(t.failed.toOption.map(stringified) == Some(stringified(template)))
  }

  test("creates dto with calculations"){
    // given
    val backlog = aBackLog(
      items = Seq(
                  aStory("do push-ups", Seq(aTeamEstimate(1))).copy(isComplete=Some(true)), 
                  aStory("run laps", Seq(aTeamEstimate(1))).copy(isComplete=Some(false), inProgress=Some(true)), 
                  anEpic("train for marathon", Seq(aTeamEstimate(1))).copy(isComplete=Some(false)), 
                  aGoal(name = "Be Super Fit")))
                  
    println(backlog.todo)
    // when
    val result = backlog.toDto("n/a").calculations
    
    // then
    
    assert(result.pointsTodo == 2)
    assert(result.pointsDone == 1)
    assert(result.pointsInProgress == 1)
  }
  
  
  private def aBackLog(projectedVelocity: Option[Int] = None, items: Seq[Item]) = Backlog(
      id = "xyz1213",
      name = "My Backlog",
      memo = "Memorable Saying",
      projectedVelocity = projectedVelocity,
      items = items
  )
  
  private def aTeamEstimate(numPoints:Int) = {
    Estimate(
        id = UUID.randomUUID().toString(),
        currency = "team",
        value = numPoints,
        when = 1
        )
  }
  private def aStory(name: String, estimates:Seq[Estimate] = Seq()): Item = Item(
    id = name,
    name = name,
    kind = "story",
    estimates = Some(estimates)
  )
  private def anEpic(name: String, estimates:Seq[Estimate] = Seq()): Item = Item(
    id = name,
    name = name,
    kind = "epic",
    estimates = Some(estimates)
  )
  private def aGoal(name: String): Item = Item(
    id = name,
    name = name,
    kind = "goal",
    estimates = None
  )
}
