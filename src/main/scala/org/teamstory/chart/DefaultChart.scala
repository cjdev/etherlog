package org.teamstory.chart

import org.teamstory.api._
import org.joda.time._
import org.joda.time.format.DateTimeFormat
import org.httpobjects.Query

object DefaultChart {
  private val dateFormat = DateTimeFormat.fullDate

  def makeSvg(stats: Seq[StatsLogEntry],
          chartWidth: Int = 50,
          chartHeight: Int = 10,
          goals: Seq[GoalData] = Seq(),
          projections: Seq[ChartProjection],
          lastTime: Long,
          options: ChartOptions = ChartOptions.LEGACY_FORMAT) = {

    val leftMargin = 2
    val rightMargin = 2
  
    val nHeight = stats.size match {
      case 0 => 0
      case _ => stats.map(entry => entry.done + entry.todo).max
    }

    val aspectRatio = chartWidth.toDouble / chartHeight.toDouble

    val chartStartTime = options.startDate match {
      case None => stats.headOption.map(_.when).getOrElse(0L)
      case Some(date) => date.toDateMidnight().getMillis()
    }

    val lastStatTime = stats.lastOption.map(_.when).getOrElse(0L)

    val chartEndTime = options.endDate match {
      case None => {
        val allTimes = projections.map(_.whenComplete.getMillis()) ++ List(lastTime, lastStatTime)
        allTimes.max
      }
      case Some(date) => date.toDateMidnight().getMillis()
    }

    val timeSpan = chartEndTime - chartStartTime
    val drawAreaWidth = chartWidth - leftMargin - rightMargin

    def xPixels(millis: Long): Double = {
      val timeDifference = (millis - chartStartTime).toDouble
      val chartPortion = timeDifference / timeSpan

      (chartPortion * drawAreaWidth) + leftMargin
    }

    def xPixelsFromInstant(when: Instant) = xPixels(when.getMillis)

    // TODO: Clean-up the margin/padding mechanisms in here
    val heightOfSpaceBelowGraph = if (options.showMonthLabels) 1.25 else 2.0
    val topBand = if (options.showMonthLabels) 0.5 else 2.0
    def storyPointsToYAxisDistance(v: Number) = (((chartHeight.doubleValue - heightOfSpaceBelowGraph - topBand) / (nHeight).doubleValue) * v.doubleValue)
    def yPixels(v: Number) = (topBand + storyPointsToYAxisDistance(v))

    val parts = if (stats.isEmpty) {
      Seq()
    } else {
      val bands = stats.tail.zipWithIndex.flatMap({
        case (entry, idx) =>

          val prev = stats(idx)
          val xLeft = xPixels(prev.when)
          val xRight = xPixels(entry.when)

          val pointsDone = if (options.showCompletedWork) {
            Seq(
              (xLeft, yPixels(nHeight - prev.total)),   // total start 
              (xRight, yPixels(nHeight - entry.total)), // total end
              (xRight, yPixels(nHeight - entry.todo)),  // remaining end
              (xLeft, yPixels(nHeight - prev.todo)))    // remaining start
          } else Seq()

          val rightEdgeDone = if (options.showCompletedWork) {
            Seq(
              (xRight, yPixels(nHeight - entry.total)), // total wat
              (xRight, yPixels(nHeight - entry.todo)))  // remaining wat
          } else Seq()


          val pointsTodo = Seq(
            (xLeft, yPixels(nHeight - prev.todo)),     // start remaining       |^
            (xLeft, yPixels(nHeight - prev.nonTeam)),     // start bottom of chart |_
            (xRight, yPixels(nHeight - entry.nonTeam)),   // end bottom of chart      _|
            (xRight, yPixels(nHeight - entry.todo)))   // end remaining            ^|

          val rightEdgeTodo = Seq(
            (xRight, yPixels(nHeight - entry.nonTeam)),
            (xRight, yPixels(nHeight - entry.todo))
          )
          
          val pointsSwag = Seq(
            (xLeft, yPixels(nHeight - prev.nonTeam)),     // start remaining       |^ 
            (xLeft, yPixels(nHeight)),                 // start bottom of chart |_ 
            (xRight, yPixels(nHeight)),                // end bottom of chart      _|
            (xRight, yPixels(nHeight - entry.nonTeam))    // end remaining            ^|
          )
          
          val rightEdgeSwag = Seq(
            (xRight, yPixels(nHeight)),
            (xRight, yPixels(nHeight - entry.nonTeam))
          )

          def print(points: Seq[(Any, Any)]) = points
                  .map(x => x._1 + "," + x._2) // to text
                  .mkString(" "); // combined

          Seq(
            """<polygon points="""" + print(rightEdgeDone) + """" class="done-right-edge"/>""",
            """<polygon points="""" + print(rightEdgeTodo) + """" class="todo-right-edge"/>""",
            """<polygon points="""" + print(rightEdgeSwag) + """" class="swag-right-edge"/>""",
            """<polygon points="""" + print(pointsDone) + """" class="done"/>""",
            """<polygon points="""" + print(pointsTodo) + """" class="todo"/>""",
            """<polygon points="""" + print(pointsSwag) + """" class="swag"/>"""
          )
      })

      // TODO: customize?
      val numLines = 5

      val ptsPerLine = (nHeight.toDouble / numLines.toDouble).ceil.intValue

      val hLines = (for (n <- 1 to numLines) yield {
        val points = (n * ptsPerLine)
        val nY = yPixels(nHeight.toDouble - points);
        if (nY > 0) {
          Seq(
            """<line y1="""" + nY + """" x1="0" y2="""" + nY + """" x2="""" + xPixels(chartEndTime) + """" />""",
            """<text x="0" y="""" + nY + """" >""" + (points) + """</text>"""
          )
        } else {
          Seq()
        }
      }).flatten.toSeq

      val firstWeekDay = options.weekStart match {
        case None => new Instant(chartStartTime).toDateTime().toDateMidnight()
        case Some(day) => day.toDateMidnight()
      }

      val weekBoundaries: Stream[DateMidnight] = {
        def loop(n: DateMidnight): Stream[DateMidnight] = {
          n #:: loop(n.plusWeeks(1));
        }
        loop(firstWeekDay)
      }

      val firstDaysOfWeeks = weekBoundaries.takeWhile(_.getMillis() <= chartEndTime).filter(_.getMillis() > chartStartTime);

      val vLines = firstDaysOfWeeks.map {
        date =>
          val title = date.toYearMonthDay().toString() + " through " + date.plusWeeks(1).toYearMonthDay().toString()
          val nX = xPixels(date.getMillis())

          val weekNumber = date.getWeekOfWeekyear()
          val isOddWeek = (weekNumber % 2 != 0)
          val line = if ((!options.drawOnlyEvenWeeks) || isOddWeek) {
            Seq( """<line x1="""" + nX + """" y1="0" x2="""" + nX + """" y2="""" + yPixels(nHeight) + """" class="verticalLine"/>""")
          } else {
            Seq()
          }

          if (options.drawWeekNumbers) {
            line.toList :+ """ <text x="""" + nX + """" y="""" + yPixels(nHeight) + """" >""" + weekNumber + """<title>""" + title + """</title></text>"""
          } else {
            line
          }
      }.flatten.toSeq


      val everyDaySinceTheStartOfTheChart: Stream[DateMidnight] = {
        def loop(n: DateMidnight): Stream[DateMidnight] = {
          n #:: loop(n.plusDays(1));
        }
        loop(new Instant(chartStartTime).toDateTime().toDateMidnight())
      }

      val monthLines = if (options.showMonthLabels) {
        everyDaySinceTheStartOfTheChart.takeWhile(_.getMillis() <= chartEndTime).filter(_.getDayOfMonth() == 1).map {
          date =>

            val monthAbbrev = date.monthOfYear().getAsText().substring(0, 3)
            val title = if (date.getMonthOfYear() == 1) monthAbbrev + " '" + date.getYearOfCentury() else monthAbbrev
            val nX = xPixels(date.getMillis())
            val nXnext = xPixels(date.plusMonths(1).getMillis())
            val width = nXnext - nX

            val lines = if (options.showMonthVerticals) {
              val spacing = .25
              Seq(
                """<line x1="""" + nX + """" y1="""" + (yPixels(nHeight) + spacing) + """" x2="""" + nX + """" y2="""" + (yPixels(nHeight) + 1 + spacing) + """" class="monthBoundary"/>"""
              )
            } else {
              Seq()
            }
            val labels = Seq( """ <text text-anchor="middle" x="""" + (nX + (width / 2)) + """" y="""" + (yPixels(nHeight) + 1) + """" >""" + title + """<title>""" + title + """</title></text>""")
            labels ++ lines
        }.flatten.toSeq
      } else {
        Seq()
      }

      val goalLines = goals.flatMap {
        goal =>
          val yVal = yPixels(nHeight - goal.points)

          val dot = if (!options.showGoalTargetDots) None
          else goal.when match {
            case Some(when) => {
              if (when >= lastStatTime) {
                val date = dateFormat.print(new DateTime(when)) + ": "

                Some( """<circle cx="""" + xPixels(when) + """" cy="""" + yVal + """" r=".25"><title>""" + date + goal.description + """</title></circle>""")
              } else {
                None
              }
            }
            case None => None
          }

          val labels = if (options.showGoalLabels) {
            Seq( """ <text class="goal-label" x="""" + xPixels(lastStatTime) + """" y="""" + yVal + """" >""" + goal.description + """<title>""" + goal.description + """</title></text>""")
          } else {
            Seq()
          }

          val hLines = if (options.showGoalHLines) {
            Seq( """<line class="projection" y1="""" + yVal + """" x1="""" + xPixels(lastStatTime) + """" y2="""" + yVal + """" x2="""" + xPixels(chartEndTime) + """" />""")
          } else {
            Seq()
          }


          val vLines = goal.whenForReal match {
            case None => Seq()
            case Some(when) => {
              if (options.showGoalVLines && (when >= lastStatTime)) {
                val date = new YearMonthDay(when)
                val text = goal.description + " - " + date.toString()
                Seq(
                  """<circle cx="""" + xPixels(when) + """" cy="""" + yVal + """" r=".25"><title>""" + text + """</title></circle>""",
                  """ <text class="goal-label" x="""" + (xPixels(when) + 1) + """" y="""" + yVal + """" >""" + text + """<title>""" + text + """</title></text>""")
                //            Seq("""<line class="projection" y1="""" + yVal + """" x1="""" + x(lastStatTime) + """" y2="""" + yVal + """" x2="""" + x(chartEndTime) + """" />""")
              } else {
                Seq()
              }
            }
          }



          hLines ++ dot ++ labels ++ vLines
      }


      val latest = stats.last
      val otherLines = projections.zipWithIndex.flatMap {
        idxAndProjection =>
          val (projection, id) = idxAndProjection
          val styleClass = if (id == 0) {
            "projection"
          } else {
            "old-projection"
          }
          Seq(
            """<line class="""" + styleClass + """" y1="""" + yPixels(nHeight - projection.pointsRemaining) + """" x1="""" + xPixelsFromInstant(projection.from) + """" y2="""" + yPixels(nHeight) + """" x2="""" + xPixelsFromInstant(projection.whenComplete) + """" />"""
          )
      }

      bands ++ hLines ++ vLines ++ goalLines ++ otherLines ++ monthLines
    }


    val text = parts.mkString(start = "  ", sep = "\n  ", end = "")

    val height = yPixels(nHeight)

    """<?xml version="1.0" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" 
  "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg xmlns="http://www.w3.org/2000/svg" version="1.1"
    viewBox="0 0 """ + chartWidth + " " + chartHeight + """">
     <style type="text/css" >
      <![CDATA[
        .swag {
            fill: yellow;
        }
        .todo {
            fill:blue;
        }
        .todo-right-edge {
            stroke:blue;
            stroke-width:.03
        }
        .done-right-edge {
            stroke:green;
            stroke-width:.03
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

        .projection, .old-projection {
            stroke:black;
            stroke-width:.05;
        }
        .old-projection {
            stroke-dasharray: .5,.5;
        }
        .monthBoundary {
            stroke:grey;
            stroke-width:.05;
        }
        .goal-label {

            font-size:.35pt;
            font-family:sans-serif;
        }
      ]]>
    </style>
""" + text + """
</svg>"""
  }
}