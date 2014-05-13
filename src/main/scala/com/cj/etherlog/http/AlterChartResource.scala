package com.cj.etherlog.http

import org.httpobjects._
import org.httpobjects.DSL._
import com.cj.etherlog.datas.Data
import com.cj.etherlog.Jackson
import com.cj.etherlog.api.BacklogStatusPatch
import com.cj.etherlog.Etherlog
import com.cj.etherlog.Jackson._
import com.cj.etherlog.Service
import org.joda.time.Months
import scala.collection.mutable.ListBuffer
import org.joda.time.Instant
import org.joda.time.YearMonthDay
import com.cj.etherlog.chart.ChartProjection
import com.cj.etherlog.datas.BacklogVersion
import com.cj.etherlog.chart.ChartOptions
import HttpUtils._
import com.cj.etherlog.Clock
import org.joda.time.DateMidnight
import com.cj.etherlog.chart.IterationStats
import com.cj.etherlog.datas.BacklogVersion
import com.cj.etherlog.chart.IterationStats

class IterationBarChartResource (data:Data, clock:Clock) extends HttpObject("/api/backlogs/{id}/chart/iteration-bars"){
  
    case class IterationDelta(val bounds:Iteration, val startingState:BacklogVersion, val endingState:BacklogVersion)
    case class Iteration(val start:DateMidnight){
        val end = start.plusDays(14)
        def next() = Iteration(end)
        def contains(timestamp:Long) = {
            val i = new Instant(timestamp);
            i.isBefore(end) && !i.isBefore(start)
        }
    }
      
    private def allVersionsOfBacklog(backlogId:String):Seq[BacklogVersion] = {
      val versions = new ListBuffer[BacklogVersion]()
      data.scanBacklogHistory(backlogId, {version=>versions += version})
      versions.toSeq
    }
    
    private def iterationDeltasForBacklog(backlogId:String, end:Instant) = {
      
      def iterationsStarting(day:YearMonthDay):Stream[Iteration] = {
              def loop(n:Iteration):Stream[Iteration] = {n#::loop(n.next)}
              loop(Iteration(day.toDateMidnight()))
      }
      
//      val allVersions:Stream[BacklogVersion] = {
//              def loop(n:BacklogVersion):Stream[BacklogVersion] = {
//                  if(n.previousVersion==null){
//                      Stream.empty
//                  }else{
//                      n#::loop(data.versions.get(n.previousVersion)); 
//                  }
//              }
//              loop(data.versions.get(data.backlogs.get(backlogId).latestVersion))
//      }
      
      val allVersions = allVersionsOfBacklog(backlogId)
      
      def publishedVersions = allVersions.filter(_.backlog.memo!="wip")
      
      
      
      
      val iterationsThisYear = iterationsStarting(new YearMonthDay(2014, 01, 01)).take(25)
      
      
      val deltas = iterationsThisYear.filter(_.start.isBefore(end)).flatMap{iteration=>
        val versionsBeforeThisIteration = publishedVersions.filter{v=>new Instant(v.when).isBefore(iteration.start)}
        val versionsThisIteration = publishedVersions.filter{v=>
          iteration.contains(v.when)
        }.toList
        
        val maybeDelta = (versionsBeforeThisIteration.headOption, versionsThisIteration.headOption) match {
          case (Some(s), Some(e)) => Some(IterationDelta(bounds=iteration, startingState = s, endingState = e))
          case (Some(s), None) => None
          case (None, Some(e)) => Some(IterationDelta(bounds=iteration, startingState = versionsThisIteration.last, endingState = e))
          case (None, None) => None
        }
        
        maybeDelta match {
          case Some(d) => println(d.bounds.start, d.bounds.end, new Instant(d.startingState.when), new Instant(d.endingState.when))
          case None=>{}
        }
        
        maybeDelta
      }
      
      deltas
    }
  
    override def get(req:Request) = {
      val id = req.path().valueFor("id")
      
      val stats = buildStatsLogFromQueryString(id, req, data, clock);
      val nowParam = req.query().valueFor("now");
      val now = if(nowParam==null) clock.now.getMillis else nowParam.toLong
      
      val endParam = req.query().valueFor("end");
      val end = if(endParam == null) now else endParam.toLong
      
      val lastTime = now + (Months.months(3).toMutablePeriod().toDurationFrom(new Instant(now)).getMillis())
      
      val iterationDeltas = iterationDeltasForBacklog(
                                  backlogId=id, new Instant(end))
      
      val iterationStats = iterationDeltas.zipWithIndex.map{deltaWithIndex=>
        val (d, i) = deltaWithIndex;
        val diff = d.endingState.delta(d.startingState)
        
        val addedThisIteration = diff.added - diff.removed + diff.reopened
        
        val todo = d.startingState.backlog.todo - diff.finished
        
        val x = if(addedThisIteration<0){
          (todo+addedThisIteration, 0)
        }else{
          (todo, addedThisIteration)
        }
        
        val whenProjectedComplete = d.endingState.projectedEnd match {
          case None=>None
          case Some(e)=> if(i>=iterationDeltas.length-2) Some(e) else None // only show from last two ... kind of hacky use of this field
        }
        
        IterationStats(
            addedThisIteration = x._2,
            todoFromLastIterationMinusDone = x._1, 
            extras=Map(),
            start=d.bounds.start.getMillis(),
            end=d.bounds.end.getMillis(),
            finished=diff.finished,
            whenProjectedComplete=whenProjectedComplete) 
      }
      
      val goals = iterationDeltas.lastOption match {
        case None => Seq()
        case Some(lastDelta) => lastDelta.endingState.backlog.goalData(lastDelta.bounds.end.getMillis())
      }
      
      Jackson.jackson.writeValue(System.out, iterationStats);
      
      val svgText = com.cj.etherlog.chart.IterationBarChart.makeSvg(
                      stats=iterationStats, 
                      lastTime = lastTime, 
                      goals=goals,
                      options=ChartOptions.fromQuery(req.query()),
                      now = new Instant(now)
                 )
      
      OK(Bytes("image/svg+xml", svgText.getBytes()))
    }
    
}