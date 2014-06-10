package org.teamstory.chart

import org.teamstory.api._
import org.teamstory.datas._
import org.scalatest.FunSuite

class BacklogTest extends FunSuite {
  private def goal(name: String) = Item(
    id = name,
    name = name,
    kind = "goal",
    estimates = None
  )

  private def storyWithEstimate(name: String, estimate: Int, done: Boolean) = Item(
    id = name,
    name = name,
    kind = "story",
    isComplete = Some(done),
    estimates = Some(Seq(
      Estimate(
        id = name + "e",
        value = estimate,
        currency = "team",
        when = 1
      )
    ))
  )

  test("goal lines are calculated as points left after accomplishment") {
    // given
    val backlog = new Backlog(
      id = "foo",
      name = "The Backlog",
      memo = "Version 1",
      items = Seq(
        storyWithEstimate(name = "put dishes in dishwasher", estimate = 10, done = false),
        storyWithEstimate(name = "put away dishes", estimate = 20, done = false),
        goal("clean dishes"),
        storyWithEstimate(name = "scrub floor", estimate = 30, done = false),
        goal("clean floors"),
        storyWithEstimate(name = "take out trash", estimate = 40, done = false),
        goal("chores done")
      )
    )

    // when
    val result = backlog.goalData(1L).map(_.points)

    // then
    assert(Seq(70, 40, 0) === result)
  }

  test("completed goals are not affected by subsequent story work") {
    // given
    val backlog = new Backlog(
      id = "foo",
      name = "The Backlog",
      memo = "Version 1",
      items = Seq(
        storyWithEstimate(name = "put dishes in dishwasher", estimate = 10, done = true),
        storyWithEstimate(name = "put away dishes", estimate = 20, done = true),
        goal("clean dishes"),
        storyWithEstimate(name = "scrub floor", estimate = 30, done = true),
        goal("clean floors"),
        storyWithEstimate(name = "take out trash", estimate = 40, done = false),
        goal("chores done")
      )
    )

    // when
    val result = backlog.goalData(1L).map(_.points)

    // then
    assert(Seq(70, 40, 0) === result)
  }

  test("goal lines works with out of order story completion") {
    // given
    val backlog = new Backlog(
      id = "foo",
      name = "The Backlog",
      memo = "Version 1",
      items = Seq(
        storyWithEstimate(name = "put dishes in dishwasher", estimate = 10, done = true),
        storyWithEstimate(name = "put away dishes", estimate = 20, done = false),
        goal("clean dishes"),
        storyWithEstimate(name = "scrub floor", estimate = 30, done = true),
        goal("clean floors"),
        storyWithEstimate(name = "take out trash", estimate = 40, done = false),
        goal("chores done")
      )
    )

    // when
    val result = backlog.goalData(1L).map(_.points)

    // then
    assert(Seq(40, 40, 0) === result)
  }

  test("no goals equals no goal lines") {
    // given
    val backlog = new Backlog(
      id = "foo",
      name = "The Backlog",
      memo = "Version 1",
      items = Seq()
    )

    // when
    val result = backlog.goalData(1L).map(_.points)

    // then
    assert(result === Seq())
  }
}