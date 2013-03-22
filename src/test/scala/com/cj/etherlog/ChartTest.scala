package com.cj.etherlog

import org.junit.Test
import org.junit.Assert._
import com.cj.etherlog.Etherlog.StatsLogEntry
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
    
    def makeSvg(stats:List[StatsLogEntry]) = {
        val leftMargin = 2;
        val topMargin = 1;
        val width = 3;
        val spacing = 1;
        def scale(x:Int) = x * 100;
      
      val guts = stats.zipWithIndex.flatMap({case (entry, idx)=>
        val x = scale(idx * (width + spacing) + leftMargin)
          List(
              """<rect x="""" + x + """" width="""" + scale(width) + "\" y=\"" + scale(topMargin) + "\" height=\"" + scale(entry.done) + """" class="done"/>""",
              """<rect x="""" + x + """" width="""" + scale(width) + "\" y=\"" + scale(topMargin + entry.done) + "\" height=\"" + scale(entry.todo) + """" class="todo"/>"""
           )
      }).mkString(start="  ", sep="\n  ", end="")
      
      
      """<?xml version="1.0" standalone="no"?>
<?xml-stylesheet href="mystyle.css" type="text/css"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" 
  "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg xmlns="http://www.w3.org/2000/svg" version="1.1"
     width="10cm" height="5cm" viewBox="0 0 1000 1000">
""" + guts + """
</svg>"""
      }
}