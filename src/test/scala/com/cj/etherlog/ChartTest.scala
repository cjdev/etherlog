package com.cj.etherlog

import org.junit.Test
import org.junit.Assert._
import com.cj.etherlog.Etherlog.StatsLogEntry
import com.cj.etherlog.Etherlog.makeSvg
import org.apache.commons.io.IOUtils

class ChartTest {
    @Test
    def emptyChart(){
      // given
      val input = List[StatsLogEntry]()
      
      // when
      val svg = makeSvg(input)
      
      // then
      assertEquals("""
<?xml version="1.0" standalone="no"?>
<?xml-stylesheet href="mystyle.css" type="text/css"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" 
  "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg xmlns="http://www.w3.org/2000/svg" version="1.1"
     width="10cm" height="5cm" viewBox="0 0 1000 1000">
  
</svg>""".trim(), svg)
    }
    
     @Test
    def twoItems(){
      // given
      val input = List[StatsLogEntry](
              StatsLogEntry(version="1", when=1, memo="", todo=2, done=1),
              StatsLogEntry(version="2", when=2, memo="", todo=1, done=2)
      )
      
      // when
      val svg = makeSvg(input)
      
      // then
      val expected = IOUtils.toString(getClass().getResourceAsStream("/my.svg"))
      assertEquals(expected, svg)
    }
     
     
     @Test
     def reallyBig(){
       
     }
     
     @Test
     def scopeIncrease(){
      // given
      val input = List[StatsLogEntry](
              StatsLogEntry(version="1", when=1, memo="", todo=2, done=1),
              StatsLogEntry(version="2", when=2, memo="", todo=3, done=1)
      )
      
      // when
      val svg = makeSvg(input)
      
      // then
      val expected = IOUtils.toString(getClass().getResourceAsStream("/scopeIncrease.svg"))
      assertEquals(expected, svg)
     }
     
     
     @Test
     def reallyTall(){
      // given
      val input = List[StatsLogEntry](
              StatsLogEntry(version="1", when=1, memo="", todo=1000, done=1000)
      )
      
      // when
      val svg = makeSvg(input)
      
      // then
      val expected = IOUtils.toString(getClass().getResourceAsStream("/reallyTall.svg"))
      assertEquals(expected, svg)
     }
     
     @Test
     def reallyWide(){
      // given
      val input = for(x <- 0 to 500) yield {
          StatsLogEntry(version=x.toString, when=x, memo="", todo=500-x, done=x)
       }
      
      // when
      val svg = makeSvg(input.toList)
      
      // then
      val expected = IOUtils.toString(getClass().getResourceAsStream("/reallyWide.svg"))
      assertEquals(expected, svg)
     }
     
     
     @Test
     def nonUniformIntervals(){
      // given
      val input = List[StatsLogEntry](
              StatsLogEntry(version="1", when=1, memo="", todo=2, done=1),
              StatsLogEntry(version="2", when=3, memo="", todo=1, done=2)
      )
      
      // when
      val svg = makeSvg(input.toList)
      
      // then
      val expected = IOUtils.toString(getClass().getResourceAsStream("/nonUniformIntervals.svg"))
      assertEquals(expected, svg)
     }
    
}