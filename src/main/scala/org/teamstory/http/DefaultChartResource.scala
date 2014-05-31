package org.teamstory.http

import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.datas.Data
import org.teamstory.Jackson
import org.teamstory.api.BacklogStatusPatch
import org.teamstory.TeamStory
import org.teamstory.Jackson._
import org.teamstory.Service
import org.joda.time.Months
import scala.collection.mutable.ListBuffer
import org.joda.time.Instant
import org.joda.time.YearMonthDay
import org.teamstory.chart.ChartProjection
import org.teamstory.datas.BacklogVersion
import org.teamstory.chart.ChartOptions
import HttpUtils._
import org.teamstory.Clock

class DefaultChartResource(data:Data, service:Service, clock:Clock) extends HttpObject("/api/backlogs/{id}/chart/default"){
    override def get(req:Request) = {
      val id = req.path().valueFor("id")
      val stats = buildStatsLogFromQueryString(id, req, data, clock);
      val nowParam = req.query().valueFor("now");
      val now = if(nowParam==null) clock.now.getMillis else nowParam.toLong
      
      val lastTime = now + (Months.months(3).toMutablePeriod().toDurationFrom(new Instant(now)).getMillis())
      
      val version = stats.head._2
      
      val myStats = stats.map(_._1).toList.reverse
      
      val pastProjection = req.query().valueFor("compareWith") match {
        case null => Seq()
        case s:String => {
          val date = new YearMonthDay(s)
          
          val matches = ListBuffer[BacklogVersion]()
          data.scanBacklogHistory(id, {version=>
            if(new Instant(version.when).isAfter(date.toDateTimeAtMidnight())){
                matches += version
            }
          })
          
          val maybeLastObservationAtDate = matches.lastOption
          maybeLastObservationAtDate match {
            case None=>Seq()
            case Some(v)=>{
              val o = v.backlog
              
              o.projectedVelocity match {
                  case Some(weeklyVelocity)=> {
                    val pointsToGoal = o.todo
                    val pointsPerWeek = weeklyVelocity
                    val numWeeksToGoal = pointsToGoal.toDouble/pointsPerWeek.toDouble
                    val numMillisInWeek = 1000 * 60 * 60 * 24 * 7;
                    
                    val millisWhenGoalComplete = (v.when + (numMillisInWeek.toDouble * numWeeksToGoal)).toLong
                    Seq(ChartProjection(
                            from=new Instant(v.when), 
                            pointsRemaining=o.todo, 
                            whenComplete=new Instant(millisWhenGoalComplete)))
                  }
                  case None=>Seq()
                }
              
            }
          }
        }
      }
      val last = myStats.last.when
      
      val projections = version.projectedEnd match {
        case None=>Seq()
        case Some(end) => Seq(ChartProjection(
                            from=new Instant(last), 
                            pointsRemaining=myStats.last.todo, 
                            whenComplete=new Instant(end)))
      }
      
      val text = org.teamstory.chart.DefaultChart.makeSvg(
                      stats=myStats, 
                      lastTime = lastTime, 
                      projections = projections ++ pastProjection,
                      goals=version.backlog.goalData(last),
                      options=ChartOptions.fromQuery(req.query())
                 )
      
      OK(Bytes("image/svg+xml", text.getBytes()))
    }
    
}