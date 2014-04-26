package com.cj.etherlog.chart

import org.junit.Test
import org.junit.Assert._
import org.apache.commons.io.IOUtils
import org.apache.commons.io.FileUtils
import java.io.File
import com.cj.etherlog.api._
import org.junit.Test
import org.joda.time.Instant
    
class ChartTest {
    
    def resourceAsString(name:String) = IOUtils.toString(getClass().getResourceAsStream(name))
  
    @Test
    def emptyChart(){
      // given
      val input = Seq[StatsLogEntry]()
      
      // when
      val svg = makeSvg(stats=input, lastTime=3, projections=Seq())
      
      // then
      val expected = resourceAsString("empty.svg")
      assertEquals(expected, svg)
    }
    
    @Test
    def realisticTimes(){
      // given
      val input = Seq[StatsLogEntry](
              StatsLogEntry(version="1", when=1364053894008L, memo="", todo=3, done=0),
              StatsLogEntry(version="2", when=1364071916138L, memo="", todo=1, done=2)
      )
      
      // when
      val svg = makeSvg(
                      stats=input, 
                      lastTime=1364089938268L, 
                      goals=Seq(GoalData(
                                  description="foo",
                                  points = 2), 
                                GoalData(
                                  description="bar",
                                  points=1, 
                                  when = Some(1364071916138L))), 
                      projections=Seq(ChartProjection(
                          from=new Instant(1364071916138L),
                          pointsRemaining=1,
                          whenComplete=new Instant(1364089938268L))))
      
      // then
      val expected = resourceAsString("my.svg").replaceAll("FORMATTED_TIME_A", "Saturday, March 23, 2013")
      assertEquals(expected, svg)
    }
    
    @Test
    def twoItemsAndGoals(){
      // given
      val input = Seq[StatsLogEntry](
              StatsLogEntry(version="1", when=1, memo="", todo=3, done=0),
              StatsLogEntry(version="2", when=2, memo="", todo=1, done=2)
      )
      
      // when
      val svg = makeSvg(
                  stats=input, 
                  lastTime=3, 
                  goals=Seq(GoalData(
                              description="foo",
                              points = 2), 
                           GoalData(
                              description="bar",
                              points=1, 
                              when = Some(2))),
                  projections=Seq(ChartProjection(
                          from=new Instant(2),
                          pointsRemaining=1,
                          whenComplete=new Instant(3)))
                )
      
      // then
      val expected = resourceAsString("my.svg").replaceAll("FORMATTED_TIME_A", "Wednesday, December 31, 1969")
      
      assertEquals(expected, svg)
    }
     
    @Test
    def threeItems(){
      // given
      val input = Seq[StatsLogEntry](
              StatsLogEntry(version="1", when=1, memo="", todo=3, done=0),
              StatsLogEntry(version="2", when=2, memo="", todo=2, done=1),
              StatsLogEntry(version="3", when=3, memo="", todo=0, done=3)
      )
      val projections =Seq(ChartProjection(
                          from=new Instant(3),
                          pointsRemaining=0,
                          whenComplete=new Instant(4)))
                          
      // when
      val svg = makeSvg(input, lastTime=4, projections = projections)
      
      // then
      val expected = resourceAsString("threeItems.svg")
      
            writeToFile(svg, new File("/tmp/actual.svg"))
      writeToFile(expected, new File("/tmp/expected.svg"))
      
      assertEquals(expected, svg)
      
    }
     
    

    @Test
    def burndownLineFollowsAScopeDecreaseWithNothingDone(){
      // given
      val input = Seq[StatsLogEntry](
              StatsLogEntry(version="1", when=1, memo="", todo=10, done=0),
              StatsLogEntry(version="2", when=2, memo="", todo=5, done=0)
      )
      
      val projections =Seq(ChartProjection(
                          from=new Instant(2),
                          pointsRemaining=5,
                          whenComplete=new Instant(3)))
      // when
      val svg = makeSvg(
                  stats=input, 
                  lastTime=3, 
                  goals=Seq(),
                  projections=projections
                )
      
      // then
      val expected = resourceAsString("scopeDecreaseWithNothingDone.svg").replaceAll("FORMATTED_TIME_A", "Wednesday, December 31, 1969")

      
      assertEquals(expected, svg)
    }
    
    @Test
    def burndownLineFollowsAScopeDecreaseWithSomethingDone(){
      // given
      val input = Seq[StatsLogEntry](
              StatsLogEntry(version="1", when=1, memo="", todo=10, done=0),
              StatsLogEntry(version="2", when=2, memo="", todo=5, done=3)
      )
      
      val projections =Seq(ChartProjection(
                          from=new Instant(2),
                          pointsRemaining=5,
                          whenComplete=new Instant(3)))
      // when
      val svg = makeSvg(
                  stats=input, 
                  lastTime=3, 
                  goals=Seq(),
                  projections=projections
                )
      
      // then
      val expected = resourceAsString("scopeDecreaseWithSomethingDone.svg").replaceAll("FORMATTED_TIME_A", "Wednesday, December 31, 1969")
      
      assertEquals(expected, svg)
    }
    
    private def writeToFile(s:String, f:File){
      FileUtils.write(f, s)
      Runtime.getRuntime().exec(Array("gnome-open", f.getAbsolutePath()))
    }
}
