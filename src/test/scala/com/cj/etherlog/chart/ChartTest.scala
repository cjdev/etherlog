package com.cj.etherlog.chart

import org.junit.Test
import org.junit.Assert._
import org.apache.commons.io.IOUtils
import org.apache.commons.io.FileUtils
import java.io.File
import com.cj.etherlog.api._
import org.junit.Test
    

class ChartTest {
    
    def resourceAsString(name:String) = IOUtils.toString(getClass().getResourceAsStream(name))
  
    @Test
    def emptyChart(){
      // given
      val input = List[StatsLogEntry]()
      
      // when
      val svg = makeSvg(input)
      
      // then
      val expected = resourceAsString("empty.svg")
      assertEquals(expected, svg)
    }
    
    @Test
    def realisticTimes(){
      println(System.currentTimeMillis())
      // given
      val input = List[StatsLogEntry](
              StatsLogEntry(version="1", when=1364053894008L, memo="", todo=3, done=0),
              StatsLogEntry(version="2", when=1364071916138L, memo="", todo=1, done=2)
      )
      
      // when
      val svg = makeSvg(input)
      
      // then
      val expected = resourceAsString("my.svg")
      assertEquals(expected, svg)
    }
    
    @Test
    def twoItems(){
      // given
      val input = List[StatsLogEntry](
              StatsLogEntry(version="1", when=1, memo="", todo=3, done=0),
              StatsLogEntry(version="2", when=2, memo="", todo=1, done=2)
      )
      
      // when
      val svg = makeSvg(input)
      
      // then
      val expected = resourceAsString("my.svg")
      assertEquals(expected, svg)
    }
     
    @Test
    def threeItems(){
      // given
      val input = List[StatsLogEntry](
              StatsLogEntry(version="1", when=1, memo="", todo=3, done=0),
              StatsLogEntry(version="2", when=2, memo="", todo=2, done=1),
              StatsLogEntry(version="3", when=3, memo="", todo=0, done=3)
      )
      
      // when
      val svg = makeSvg(input)
      
      // then
      val expected = resourceAsString("threeItems.svg")
      assertEquals(expected, svg)
      
    }
}