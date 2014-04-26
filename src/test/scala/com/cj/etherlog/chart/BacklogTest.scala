package com.cj.etherlog.chart

import org.junit.Test
import com.cj.etherlog._
import org.junit.Assert

class BacklogTest {

    private def goal(name:String) = Item(
      id = name, 
      name = name, 
      kind = "goal", 
      estimates = None
    )

  
  private def storyWithEstimate(name:String, estimate:Int, done:Boolean) = Item(
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
  
  @Test
  def goalLinesAreCalculatedAsPointsLeftAfterAccomplishment () {
    // given
    val backlog = new Backlog(
        id = "foo",
        name = "The Backlog",
        memo = "Version 1",
        items = Seq(
            storyWithEstimate(name="put dishes in dishwasher", estimate=10, done=false),
            storyWithEstimate(name="put away dishes", estimate=20, done=false),
            goal("clean dishes"),
            storyWithEstimate(name="scrub floor", estimate=30, done=false),
            goal("clean floors"),
            storyWithEstimate(name="take out trash", estimate=40, done=false),
            goal("chores done")
        )
    )
    
    // when
    val result = backlog.goalData(1L).map(_.points)
    
    // then
    Assert.assertEquals(Seq(70, 40, 0), result)
  }
  
    
  @Test
  def completedGoalsAreNotAffectedBySubsequentStoryWork () {
    // given
    val backlog = new Backlog(
        id = "foo",
        name = "The Backlog",
        memo = "Version 1",
        items = Seq(
            storyWithEstimate(name="put dishes in dishwasher", estimate=10, done=true),
            storyWithEstimate(name="put away dishes", estimate=20, done=true),
            goal("clean dishes"),
            storyWithEstimate(name="scrub floor", estimate=30, done=true),
            goal("clean floors"),
            storyWithEstimate(name="take out trash", estimate=40, done=false),
            goal("chores done")
        )
    )
    
    // when
    val result = backlog.goalData(1L).map(_.points)
    
    // then
    Assert.assertEquals(Seq(70, 40, 0), result)
  }
    
  @Test
  def goalLinesWorksWithOutOfOrderStoryCompletion () {
    // given
    val backlog = new Backlog(
        id = "foo",
        name = "The Backlog",
        memo = "Version 1",
        items = Seq(
            storyWithEstimate(name="put dishes in dishwasher", estimate=10, done=true),
            storyWithEstimate(name="put away dishes", estimate=20, done=false),
            goal("clean dishes"),
            storyWithEstimate(name="scrub floor", estimate=30, done=true),
            goal("clean floors"),
            storyWithEstimate(name="take out trash", estimate=40, done=false),
            goal("chores done")
        )
    )
    
    // when
    val result = backlog.goalData(1L).map(_.points)
    
    // then
    Assert.assertEquals(Seq(40, 40, 0), result)
  }
  
  
  @Test
  def noGoalsEqualsNoGoalLines () {
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
    Assert.assertEquals(result, Seq())
  }
}