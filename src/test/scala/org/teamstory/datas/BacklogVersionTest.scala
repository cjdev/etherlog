package org.teamstory.datas

import org.junit.Test
import org.junit.Test
import org.junit.Assert
import org.teamstory.datas._
import org.teamstory.api._
import java.util.UUID

class BacklogVersionTest {
  
  @Test
  def goalsAreIgnoredInDeltas = {
    // given
    val before = aBackLogVersion(
                when=1, 
                projectedVelocity=Some(1), 
                items=Seq(aGoal(name="Some Goal")))
    val after = before.copy(when=2, backlog=before.backlog.copy(items=Seq(aGoal(name="Some Other Goal"))))
    
    // when
    val diff = after.delta(before)
    
    // then
    val expected = Delta(from=before.versionNameAndTime, 
                         to=after.versionNameAndTime, 
                         added=ItemGroupSynopsis(Seq(), 0), 
                         removed=ItemGroupSynopsis(Seq(), 0), 
                         finished=ItemGroupSynopsis(Seq(), 0),
                         reopened=ItemGroupSynopsis(Seq(), 0),
                         reestimated=ItemGroupSynopsis(Seq(), 0))
                         
    Assert.assertEquals(expected, diff)
  }
  
  @Test
  def addedEpicsAreTracked = {
    // given
    val epic = epicWithEstimate(name="Do Something", estimate=1, done = false)
    val before = aBackLogVersion(
                when=1, 
                projectedVelocity=Some(1), 
                items=Seq())
    val after = before.copy(when=2, backlog=before.backlog.copy(items=Seq(epic)))
    
    // when
    val diff = after.delta(before)
    
    // then
    val expected = Delta(from=before.versionNameAndTime, 
                         to=after.versionNameAndTime, 
                         added=ItemGroupSynopsis(Seq(ItemIdAndShortName(epic.id, "Do Something")), 1), 
                         removed=ItemGroupSynopsis(Seq(), 0), 
                         finished=ItemGroupSynopsis(Seq(), 0),
                         reopened=ItemGroupSynopsis(Seq(), 0),
                         reestimated=ItemGroupSynopsis(Seq(), 0))
                         
    Assert.assertEquals(expected, diff)
  }
  
  @Test
  def deletedEpicsAreTracked = {
    // given
    val epic = epicWithEstimate(name="Do Something", estimate=1, done = false)
    val before = aBackLogVersion(
                when=1, 
                projectedVelocity=Some(1), 
                items=Seq(
                            epic
                ))
    val after = before.copy(when=2, backlog=before.backlog.copy(items=Seq()))
    
    // when
    val diff = after.delta(before)
    
    // then
    val expected = Delta(from=before.versionNameAndTime, 
                         to=after.versionNameAndTime, 
                         added=ItemGroupSynopsis(Seq(), 0), 
                         removed=ItemGroupSynopsis(Seq(ItemIdAndShortName(epic.id, "Do Something")), 1), 
                         finished=ItemGroupSynopsis(Seq(), 0),
                         reopened=ItemGroupSynopsis(Seq(), 0),
                         reestimated=ItemGroupSynopsis(Seq(), 0))
                         
    Assert.assertEquals(expected, diff)
  }
  
  @Test
  def whenStoriesDontChangeTheyDontChange = {
    // given
    val before = aBackLogVersion(
                when=1, 
                projectedVelocity=Some(1), 
                items=Seq(
                            storyWithEstimate(name="Do Something", estimate=1, done = false)
                ))
    val after = before.copy(when=2)
    
    // when
    val diff = after.delta(before)
    
    // then
    val expected = Delta(from=before.versionNameAndTime, 
                         to=after.versionNameAndTime, 
                         added=ItemGroupSynopsis(Seq(), 0), 
                         removed=ItemGroupSynopsis(Seq(), 0), 
                         finished=ItemGroupSynopsis(Seq(), 0),
                         reopened=ItemGroupSynopsis(Seq(), 0),
                         reestimated=ItemGroupSynopsis(Seq(), 0))
                         
    Assert.assertEquals(expected, diff)
  }
  
  @Test
  def deltasIncludeChangesToEstimates = {
    // given
    val initialEstimate = Estimate(
                        id = UUID.randomUUID().toString,
                        value = 1,
                        currency = "team",
                        when = 1
                   )
    val secondEstimate = initialEstimate.copy(when=2, value=2)
    
    val story = Item(
      id = UUID.randomUUID().toString, 
      name = "Do it", 
      kind = "story", 
      isComplete = Some(false),
      estimates = Some(Seq(initialEstimate))
    )
    val before = aBackLogVersion(
                when=1, 
                items=Seq(story))
    
    val after = aBackLogVersion(
                when=2, 
                items=Seq(story.copy(estimates=Some(Seq(initialEstimate, secondEstimate)))))
                
    // when
    val diff = after.delta(before)
    
    // then
    val expected = Delta(from=before.versionNameAndTime, 
                         to=after.versionNameAndTime, 
                         added=ItemGroupSynopsis(Seq(), 0), 
                         removed=ItemGroupSynopsis(Seq(), 0), 
                         finished=ItemGroupSynopsis(Seq(), 0),
                         reopened=ItemGroupSynopsis(Seq(), 0),
                         reestimated=ItemGroupSynopsis(Seq(ItemIdAndShortName(story.id, story.name)), 1))
                         
    Assert.assertEquals(expected, diff)
  }
  
  @Test
  def theProjectNeverEndsIfThereIsNoVelocity= {
    // given
    val v = aBackLogVersion(
                when=1000L, 
                projectedVelocity=None, 
                items=Seq(
                            storyWithEstimate(name="Do Something", estimate=1, done = false)
                ))
    // when
    val result = v.projectedEnd
    
    // then
    Assert.assertEquals(None, result)
  }
  
  @Test
  def projectedEndIsRelativeToTheVersionDate= {
    // given
    val v = aBackLogVersion(
                when=1000L, 
                projectedVelocity=Some(1), 
                items=Seq(
                            storyWithEstimate(name="Do Something", estimate=1, done = false)
                ))
    // when
    val result = v.projectedEnd
    
    // then
    Assert.assertEquals(Some(1000 + oneStandardWeekOfMillis), result)
  }
    
  @Test
  def projectedEndOnlyConsidersUnfinishedWork = {
    // given
    val v =  aBackLogVersion(
                when=0L, 
                projectedVelocity=Some(1), 
                items=Seq(
                    storyWithEstimate(name="Do Something", estimate=20, done = true),
                    storyWithEstimate(name="Do Something", estimate=1, done = false)
                )) 
      
    // when
    val result = v.projectedEnd
    
    // then
    Assert.assertEquals(Some(oneStandardWeekOfMillis), result)
  }
    
  @Test
  def atTheRateOfOneStoryPerWeek_comma_aOneStoryBacklogWillFinishInOneWeek = {
    
    // given
    val v =  aBackLogVersion(
                when=0L, 
                projectedVelocity=Some(1), 
                items=Seq(
                            storyWithEstimate(name="Do Something", estimate=1, done = false)
                )) 
                
    // when
    val result = v.projectedEnd
    
    // then
    Assert.assertEquals(Some(oneStandardWeekOfMillis), result)
  }
  
    
  @Test
  def twentyOneStoryBacklogAtFiveStoriesPerWeek = {
    // given
    val v =  aBackLogVersion(
                when=0L, 
                projectedVelocity=Some(5), 
                items=Seq(
                            storyWithEstimate(name="Mix", estimate=5, done = false),
                            storyWithEstimate(name="Bake", estimate=2, done = false),
                            storyWithEstimate(name="Serve", estimate=13, done = false),
                            storyWithEstimate(name="Cleanup", estimate=1, done = false)
                )) 
                
    // when
    val result = v.projectedEnd
    
    // then
    val expected = ((21.0/5.0) * oneStandardWeekOfMillis).toLong
    Assert.assertEquals(Some(expected), result)
  }
  
  val oneStandardWeekOfMillis = (7 * 24 * 60 * 60 * 1000).toLong
    
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
    
    private def aGoal(name:String):Item = Item(
      id = name, 
      name = name, 
      kind = "goal",
      estimates = None
    ) 
    
   private def epicWithEstimate(name:String, estimate:Int, done:Boolean) = Item(
      id = name, 
      name = name, 
      kind = "epic", 
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
    
  private def aBackLogVersion(when:Long, projectedVelocity:Option[Int] = None, items:Seq[Item]) = BacklogVersion(
                id="whatever",
                when = when,
                isPublished = true, 
                previousVersion = null,
                backlog = Backlog(
                    id="xyz1213",
                    name="My Backlog",
                    memo="Memorable Saying",
                    projectedVelocity = projectedVelocity,
                    items = items
                )
               )

}