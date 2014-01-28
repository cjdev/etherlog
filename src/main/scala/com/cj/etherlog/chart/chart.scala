package com.cj.etherlog

import com.cj.etherlog.api._
import org.joda.time._
import org.joda.time.format.DateTimeFormat
    
package object chart {
    private val dateFormat = DateTimeFormat.fullDate
    
    def makeSvg(stats:Seq[StatsLogEntry], chartWidth:Int = 50, chartHeight:Int = 10, goals:Seq[GoalData] = Seq(), whenProjectedComplete:Long, lastTime:Long) = {
        val leftMargin = 2;
        val rightMargin = 2;
        val topMargin = 1;
        val bottomMargin = 1;
        val spacing = 1;
        
        val nHeight = stats.size match {
          case 0=>0;
          case _=> stats.map(entry=>entry.done + entry.todo).max
        }
        
        val aspectRatio = chartWidth.toDouble/chartHeight.toDouble
        
        val start = stats.headOption.map(_.when).getOrElse(0L)
        val lastStatTime = stats.lastOption.map(_.when).getOrElse(0L)
        
        val chartEndTime = Math.max(lastTime, lastStatTime)
        
        val timeSpan = chartEndTime - start
        val drawAreaWidth = chartWidth - leftMargin - rightMargin;
        val totalWidth = chartWidth
        
        def x(millis:Long) = {
          val d = (millis - start).toDouble
          val r = d/timeSpan
          val w = (r * drawAreaWidth) + leftMargin
          
          w
        }
        
        def y(v:Number) = ((chartHeight.doubleValue/(nHeight+ topMargin + bottomMargin).doubleValue) * v.doubleValue) 
        
        val parts = if(stats.isEmpty){
          Seq()
        }else {
          val bands = stats.tail.zipWithIndex.flatMap({case (entry, idx)=>
          
            val prev = stats(idx)
            val xLeft = x(prev.when)
            val xRight = x(entry.when)
            
            val pointsTodo = Seq(
                (xLeft, y(topMargin + (nHeight-prev.total))),
                (xRight, y(topMargin + (nHeight-entry.total))),
                (xRight, y(topMargin + (nHeight-entry.todo))),
                (xLeft, y(topMargin + (nHeight-prev.todo))))
                
                
            val pointsDone = Seq(
                (xLeft, y(topMargin + (nHeight-prev.todo))),
                (xLeft, y(topMargin + nHeight)),
                (xRight, y(topMargin + nHeight)),
                (xRight, y(topMargin + (nHeight-entry.todo))))
            
                
            def print(points:Seq[(Any, Any)]) = points
                                .map(x=>x._1 + "," + x._2) // to text
                                .mkString(" "); // combined
            
            Seq(
                 """<polygon points="""" + print(pointsTodo) + """" class="done"/>""",
                 """<polygon points="""" + print(pointsDone) + """" class="todo"/>"""
               )
        })
        
        val numLines = 5;
        
        val ptsPerLine = (nHeight.toDouble/numLines.toDouble).ceil.intValue
        
        val hLines = (for(n<-1 to numLines) yield {
            val points = (n*ptsPerLine)
            val nY = y(nHeight.toDouble - points + topMargin);
            if(nY>0){
                Seq(
                        """<line y1="""" + nY + """" x1="0" y2="""" + nY + """" x2="""" + x(chartEndTime) + """" />""",
                        """<text x="0" y="""" + nY + """" >""" + (points )+ """</text>"""
                        )
            }else{
              Seq()
            }
        }).flatten.toSeq
        
        val dayBoundaries:Stream[DateMidnight] = {
                def loop(n:DateMidnight):Stream[DateMidnight] = {
                        n#::loop(n.plusWeeks(1)); }
                loop(new Instant(start).toDateTime().toDateMidnight())
        }

        val days = dayBoundaries.takeWhile(_.getMillis() <= chartEndTime).filter(_.getMillis()>start);
        
        val vLines = days.map{date=>
            val title = date.toYearMonthDay().toString() + " through " + date.plusWeeks(1).toYearMonthDay().toString()
            val nX = x(date.getMillis())
            Seq(
                """<line x1="""" + nX + """" y1="0" x2="""" + nX + """" y2="""" + y(nHeight) + """" class="verticalLine"/>""",
                """ <text x="""" + nX + """" y="""" + y(nHeight) + """" >""" + date.getWeekOfWeekyear() + """<title>""" + title + """</title></text>"""
            )
        }.flatten.toSeq
        
        val goalLines = goals.flatMap {goal=>
          val yVal = y(topMargin + (nHeight - goal.points))
          
          val dot = goal.when match {
            case Some(when)=> {
              val date = goal.when match {
                case Some(millis)=> dateFormat.print(new DateTime(millis)) + ": "
                case None => ""
              }
              Some("""<circle cx="""" + x(when) + """" cy="""" + yVal + """" r=".25"><title>""" + date + goal.description  + """</title></circle>""")
            }
            case None => None
          }
          
          Seq("""<line class="projection" y1="""" + yVal + """" x1="""" + x(start) + """" y2="""" + yVal + """" x2="""" + x(chartEndTime) + """" />""") ++ dot
        }
        
        
        val latest = stats.last
        println("SCOPE: " + latest.done)
        val otherLines = if(whenProjectedComplete>0){
          Seq(
                """<line class="projection" y1="""" + y(topMargin + nHeight - latest.todo) + """" x1="""" + x(latest.when) + """" y2="""" + y(topMargin + nHeight) + """" x2="""" + x(whenProjectedComplete) + """" />"""
          )
        }else{
          Seq()
        }
        
        bands ++ hLines ++ vLines ++ goalLines ++ otherLines     
      }
        
        
      val text = parts.mkString(start="  ", sep="\n  ", end="")

      val height = y(nHeight)
      
      """<?xml version="1.0" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" 
  "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg xmlns="http://www.w3.org/2000/svg" version="1.1"
    viewBox="0 0 """ + chartWidth + " " + chartHeight + """">
     <style type="text/css" >
      <![CDATA[
        .todo {
            fill:blue;
        }
        
        .done {
            fill:green;
        }
        text {
            font-size:.5pt;
            font-family:sans-serif;
        }
        xlabel {
            transform:rotate(30 20,40);
        }
        line {
            stroke:grey;
            stroke-width:.01
        }
    
        nowLine {
            stroke:red;
            stroke-width:.05
        }
        
        .dot {
            stroke:black;
            stroke-width:.01;
            fill:black;
        }
        
        .projection {
            stroke:black;
            stroke-width:.05;
        }
      ]]>
    </style>
""" + text + """
</svg>"""
      }
}