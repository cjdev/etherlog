package com.cj.etherlog.data

import org.junit.Test
import org.junit.Test
import org.junit.Assert
import com.cj.etherlog.Item
import com.cj.etherlog.Backlog
import com.cj.etherlog.Estimate

class BacklogVersionTest {

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
    
    
  private def aBackLogVersion(when:Long, projectedVelocity:Option[Int], items:Seq[Item]) = BacklogVersion(
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