package org.teamstory.chart

import org.teamstory.api._
import org.joda.time._
import org.joda.time.format.DateTimeFormat
import org.httpobjects.Query

case class IterationStats (
    val todoFromLastIterationMinusDone:Int,
    val addedThisIteration:Int,
    val extras:Map[String, Int],
    val start:Long,
    val end:Long,
    val finished:Int,
    val whenProjectedComplete:Option[Long]) {

  def scope = todoFromLastIterationMinusDone + addedThisIteration
}

object IterationBarChart {
    private val dateFormat = DateTimeFormat.fullDate

    def makeSvg(stats:Seq[IterationStats],
                chartWidth:Int = 50,
                chartHeight:Int = 10,
                goals:Seq[GoalData] = Seq(),
                lastTime:Long,
                now:Instant,
                options:ChartOptions = ChartOptions.LEGACY_FORMAT) = {

        val leftMargin = 2;
        val rightMargin = 2;
        val spacing = 1;

        val nHeight = stats.size match {
          case 0=>0;
          case _=> stats.map(entry=>entry.scope).max
        }

        val aspectRatio = chartWidth.toDouble/chartHeight.toDouble

        val start = options.startDate match {
          case None => stats.headOption.map(_.start).getOrElse(0L)
          case Some(date) => date.toDateMidnight().getMillis()
        }

        val lastStatTime = stats.lastOption.map(_.end).getOrElse(0L)

        val chartEndTime = options.endDate match {
          case None => {
            val allTimes = stats.flatMap(_.whenProjectedComplete) ++ List(lastTime, lastStatTime)
            allTimes.max
          }
          case Some(date) => date.toDateMidnight().getMillis()
        }

        val timeSpan = chartEndTime - start
        val drawAreaWidth = chartWidth - leftMargin - rightMargin;
        val totalWidth = chartWidth

        def x(millis:Long) = {
          val d = (millis - start).toDouble
          val r = d/timeSpan
          val w = (r * drawAreaWidth) + leftMargin

          w
        }

        def xi(when:Instant) = x(when.getMillis)

        // TODO: Clean-up the margin/padding mechanisms in here
        val heightOfSpaceBelowGraph = if(options.showMonthLabels) 1.25 else 2.0
        val topBand = if(options.showMonthLabels) 0.5 else 2.0
        def storyPointsToYAxisDistance(v:Number) = (((chartHeight.doubleValue - heightOfSpaceBelowGraph - topBand)/(nHeight).doubleValue) * v.doubleValue)
        def y(v:Number) = (topBand + storyPointsToYAxisDistance(v))

        val parts = if(stats.isEmpty){
          Seq()
        }else {
          val bands = stats.tail.zipWithIndex.flatMap({case (entry, idx)=>

            Seq()
        })

        val numLines = 5;

        val ptsPerLine = (nHeight.toDouble/numLines.toDouble).ceil.intValue

        val hLines = (for(n<-1 to numLines) yield {
            val points = (n*ptsPerLine)
            val nY = y(nHeight.toDouble - points);
            if(nY>0){
                Seq(
                    """<line y1="""" + nY + """" x1="0" y2="""" + nY + """" x2="""" + x(chartEndTime) + """" />""",
                    """<text x="0" y="""" + nY + """" >""" + (points )+ """</text>"""
                        )
            }else{
              Seq()
            }
        }).flatten.toSeq

        val firstWeekDay = options.weekStart match {
          case None=> new Instant(start).toDateTime().toDateMidnight()
          case Some(day)=>day.toDateMidnight()
        }

        val weekBoundaries:Stream[DateMidnight] = {
                def loop(n:DateMidnight):Stream[DateMidnight] = {
                        n#::loop(n.plusWeeks(1)); }
                loop(firstWeekDay)
        }

        val firstDaysOfWeeks = weekBoundaries.takeWhile(_.getMillis() <= chartEndTime).filter(_.getMillis()>start);

        val vLines = firstDaysOfWeeks.map{date=>
            val title = date.toYearMonthDay().toString() + " through " + date.plusWeeks(1).toYearMonthDay().toString()
            val nX = x(date.getMillis())

            val weekNumber = date.getWeekOfWeekyear()
            val isOddWeek = (weekNumber % 2 != 0)
            val line = if((!options.drawOnlyEvenWeeks) || isOddWeek){
              Seq("""<line x1="""" + nX + """" y1="0" x2="""" + nX + """" y2="""" + y(nHeight) + """" class="verticalLine"/>""")
            } else {
              Seq()
            }

            if(options.drawWeekNumbers){
               line.toList :+ """ <text x="""" + nX + """" y="""" + y(nHeight) + """" >""" + weekNumber + """<title>""" + title + """</title></text>"""
            }else{
              line
            }
        }.flatten.toSeq


        val everyDaySinceTheStartOfTheChart:Stream[DateMidnight] = {
                def loop(n:DateMidnight):Stream[DateMidnight] = {
                        n#::loop(n.plusDays(1)); }
                loop(new Instant(start).toDateTime().toDateMidnight())
        }

        val monthLines = if(options.showMonthLabels){everyDaySinceTheStartOfTheChart.takeWhile(_.getMillis() <= chartEndTime).filter(_.getDayOfMonth()==1).map{date=>

            val monthAbbrev = date.monthOfYear().getAsText().substring(0, 3)
            val title = if(date.getMonthOfYear()==1) monthAbbrev + " '" + date.getYearOfCentury() else monthAbbrev
            val nX = x(date.getMillis())
            val nXnext = x(date.plusMonths(1).getMillis())
            val width = nXnext-nX

            val lines = if(options.showMonthVerticals){
              val spacing = .25
              Seq(
                  """<line x1="""" + nX + """" y1="""" + (y(nHeight) + spacing) + """" x2="""" + nX + """" y2="""" + (y(nHeight) + 1 + spacing) + """" class="monthBoundary"/>"""
                  )
            }else{
              Seq()
            }
            val labels = Seq(""" <text text-anchor="middle" x="""" + (nX + (width/2)) + """" y="""" + (y(nHeight) + 1) + """" >""" + title + """<title>""" + title + """</title></text>""")
            labels ++ lines
        }.flatten.toSeq}else{
          Seq()
        }

        val bars = stats.map{iterationStats=>

            val end = new Instant(iterationStats.end).toDateTime()
            val start = new Instant(iterationStats.start).toDateTime().plusDays(1)

            val yValTodo = y(nHeight-iterationStats.todoFromLastIterationMinusDone)
            val yValAdded = y(nHeight-iterationStats.todoFromLastIterationMinusDone-iterationStats.addedThisIteration)
            val x1 = x(start.getMillis())
            val x2 = x(end.getMillis())


            val todoBar = Seq(
                (x1, yValTodo),
                (x2, yValTodo),
                (x2, y(nHeight)),
                (x1, y(nHeight)))

            val addedBar = Seq(
                (x1, yValTodo),
                (x2, yValTodo),
                (x2, yValAdded),
                (x1, yValAdded))

            def print(points:Seq[(Any, Any)]) = points
                                .map(x=>x._1 + "," + x._2) // to text
                                .mkString(" "); // combined

            val extraClasses = if(now.isBefore(end)){
              "in-progress"
            }else{
              ""
            }

            Seq(
                 s"""<polygon points="${print(todoBar)}" class="todo $extraClasses"/>""",
                 s"""<polygon points="${print(addedBar)}" class="added $extraClasses"/>"""
               )
        }

        val goalLines = goals.flatMap {goal=>
          val yVal = y(nHeight - goal.points)

          val vLines = goal.whenForReal match {
            case None => Seq()
            case Some(when) => {
              if(options.showGoalVLines && (when >=lastStatTime)){
                val date = new YearMonthDay(when)
                val text = goal.description + " - " + date.toString()
                Seq(
                    """<circle cx="""" + x(when) + """" cy="""" + yVal + """" r=".25"><title>""" + text  + """</title></circle>""",
                    """ <text class="goal-label" x="""" + (x(when) +1) + """" y="""" + yVal + """" >""" + text + """<title>""" + text + """</title></text>""")
              } else {
                Seq()
              }
            }
          }



          vLines
        }


        val latest = stats.last
        val otherLines = stats.zipWithIndex.flatMap{idxAndProjection=>
          val (iteration, id) = idxAndProjection
          val styleClass = if(id==0) {
            "projection"
          }else{
            "old-projection"
          }
          iteration.whenProjectedComplete match {
            case None=>Seq()
            case Some(whenProjectedComplete)=> {
                Seq(
                   """<line class="""" + styleClass + """" y1="""" + y(nHeight - iteration.scope) + """" x1="""" + x(iteration.end) + """" y2="""" + y(nHeight) + """" x2="""" + x(whenProjectedComplete) + """" />"""
                )
            }
          }
        }

        bars ++ bands ++ hLines ++ vLines ++ goalLines ++ otherLines ++ monthLines
      }


      val text = parts.mkString(start="  ", sep="\n  ", end="")

      val height = y(nHeight)

      """<?xml version="1.0" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN"
  "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg xmlns="http://www.w3.org/2000/svg" version="1.1"
    viewBox="0 0 """ + chartWidth + " " + chartHeight + """" class="iteration-bars">
     <style type="text/css" >
      <![CDATA[
        .iteration-bars .todo {
            fill:blue;
        }
        .iteration-bars .todo-right-edge {
            stroke:blue;
            stroke-width:.03
        }
        .iteration-bars .done-right-edge {
            stroke:green;
            stroke-width:.03
        }
        .iteration-bars .done {
            fill:green;
        }
        .iteration-bars .added {
            fill:orange;
        }
        .iteration-bars .in-progress {
            opacity:.5;
        }
        .iteration-bars text {
            font-size:.5pt;
            font-family:sans-serif;
        }
        .iteration-bars xlabel {
            transform:rotate(30 20,40);
        }
        .iteration-bars line {
            stroke:grey;
            stroke-width:.01
        }
        .iteration-bars nowLine {
            stroke:red;
            stroke-width:.05
        }
        .iteration-bars .dot {
            stroke:black;
            stroke-width:.01;
            fill:black;
        }
        .iteration-bars .projection, .iteration-bars .old-projection {
            stroke:black;
            stroke-width:.05;
        }
        .iteration-bars .old-projection {
            stroke-dasharray: .5,.5;
        }
        .iteration-bars .monthBoundary {
            stroke:grey;
            stroke-width:.05;
        }
        .iteration-bars .goal-label {

            font-size:.35pt;
            font-family:sans-serif;
        }
      ]]>
    </style>
""" + text + """
</svg>"""
      }
}
