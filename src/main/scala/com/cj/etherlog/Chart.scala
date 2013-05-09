package com.cj.etherlog

package object chart {
    
    import com.cj.etherlog.api._
    import org.joda.time._
    
    def makeSvg(stats:List[StatsLogEntry], chartWidth:Int = 50, chartHeight:Int = 10, goals:List[Int] = List(), whenProjectedComplete:Long, lastTime:Long) = {
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
        
        val start = stats.firstOption.map(_.when).getOrElse(0L)
        val lastStatTime = stats.lastOption.map(_.when).getOrElse(0L)
        
        val chartEndTime = Math.max(lastTime, lastStatTime)
        
        val timeSpan = chartEndTime - start
        val drawAreaWidth = chartWidth - leftMargin - rightMargin;
        val totalWidth = chartWidth
        
//        val width = if(stats.isEmpty)0 else ((end - start)/stats.size).toInt;
        
        def x(millis:Long) = {
          val d = (millis - start).toDouble
          val r = d/timeSpan
          val w = (r * drawAreaWidth) + leftMargin
          
          w
        }
        
        def y(v:Number) = ((chartHeight.doubleValue/(nHeight+ topMargin + bottomMargin).doubleValue) * v.doubleValue) 
        
        val parts = if(stats.isEmpty){
          List()
        }else {
          val bands = stats.tail.zipWithIndex.flatMap({case (entry, idx)=>
          
            val prev = stats(idx)
            val xLeft = x(prev.when)
            val xRight = x(entry.when)
            
            val pointsTodo = List(
                (xLeft, y(topMargin + (nHeight-prev.total))),
                (xRight, y(topMargin + (nHeight-entry.total))),
                (xRight, y(topMargin + (nHeight-entry.todo))),
                (xLeft, y(topMargin + (nHeight-prev.todo))))
                
                
            val pointsDone = List(
                (xLeft, y(topMargin + (nHeight-prev.todo))),
                (xLeft, y(topMargin + nHeight)),
                (xRight, y(topMargin + nHeight)),
                (xRight, y(topMargin + (nHeight-entry.todo))))
            
                
            def print(points:List[(Any, Any)]) = points
                                .map(x=>x._1 + "," + x._2) // to text
                                .mkString(" "); // combined
            
            List(
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
                List(
                        """<line y1="""" + nY + """" x1="0" y2="""" + nY + """" x2="""" + x(chartEndTime) + """" />""",
                        """<text x="0" y="""" + nY + """" >""" + (points )+ """</text>"""
                        )
            }else{
              List()
            }
        }).flatten.toList
        
        val dayBoundaries:Stream[DateMidnight] = {
                def loop(n:DateMidnight):Stream[DateMidnight] = {
                        n#::loop(n.plusWeeks(1)); }
                loop(new Instant(start).toDateTime().toDateMidnight())
        }

        val days = dayBoundaries.takeWhile(_.getMillis() <= chartEndTime).filter(_.getMillis()>start);
        
        val vLines = days.map{date=>
            val nX = x(date.getMillis())
            List(
                """<line x1="""" + nX + """" y1="0" x2="""" + nX + """" y2="""" + y(nHeight) + """" class="verticalLine"/>""",
                """ <text x="""" + nX + """" y="""" + y(nHeight) + """" >""" + date.getWeekOfWeekyear() + """</text>"""
            )
        }.flatten.toList
        
        val goalLines = goals.map {goal=>
          val yVal = y(topMargin + (nHeight - goal))
          """<line class="projection" y1="""" + yVal + """" x1="""" + x(start) + """" y2="""" + yVal + """" x2="""" + x(chartEndTime) + """" />"""              
        }
        
        val latest = stats.last
        val otherLines = List(
//            """<line class="projection" y1="""" + y(topMargin + latest.done) + """" x1="""" + x(latest.when) + """" y2="""" + y(topMargin + latest.total) + """" x2="""" + x(whenProjectedComplete) + """" />"""
        )
        
        bands ::: hLines ::: vLines ::: otherLines ::: goalLines     
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
            font-size:1pt;
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
            stroke:red;
            stroke-width:.05
        }
      ]]>
    </style>
""" + text + """
</svg>"""
      }
}