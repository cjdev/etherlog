package com.cj.etherlog

import org.junit.Test
import org.junit.Assert._
import com.cj.etherlog.Etherlog.StatsLogEntry
import com.cj.etherlog.Etherlog.makeSvg
import org.apache.commons.io.IOUtils
import org.apache.commons.io.FileUtils
import java.io.File

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
<svg xmlns="http://www.w3.org/2000/svg" version="1.1" preserveAspectRatio="none" 
     width="10cm" height="10cm" viewBox="0 0 1004 2">
  
</svg>""".trim(), svg)
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
      FileUtils.write(new File("/home/stu/Desktop/temp.svg"), svg);
      val expected = IOUtils.toString(getClass().getResourceAsStream("/my.svg"))
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
      FileUtils.write(new File("/home/stu/Desktop/temp.svg"), svg);
      val expected = IOUtils.toString(getClass().getResourceAsStream("/my.svg"))
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
      //file:///home/stu/projects/cj/etherlog/src/test/resources/threeItems.svg
      
      // when
      val svg = makeSvg(input)
      
      // then
      val expected = IOUtils.toString(getClass().getResourceAsStream("/threeItems.svg"))
      assertEquals(expected, svg)
      
    }
}