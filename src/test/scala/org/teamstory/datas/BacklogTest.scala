package org.teamstory.datas

import org.scalatest.FunSuite
import org.teamstory.api._
import java.util.UUID

class BacklogTest extends FunSuite {
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
