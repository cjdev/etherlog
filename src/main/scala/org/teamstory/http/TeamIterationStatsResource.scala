package org.teamstory.http

import org.httpobjects._
import org.httpobjects.DSL._
import org.teamstory.datas.Data
import org.teamstory.Jackson
import org.teamstory.api.BacklogStatusPatch
import org.teamstory.api.IterationDto
import org.teamstory.api._
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
import org.joda.time.DateMidnight
import org.teamstory.chart.IterationStats
import org.teamstory.datas.BacklogVersion
import org.teamstory.chart.IterationStats

class TeamIterationStatsResource (data:Data, clock:Clock) extends HttpObject("/api/backlogs/{id}/iteration-stats"){
  
    case class IterationDelta(val bounds:Iteration, val startingState:BacklogVersion, val endingState:BacklogVersion)
    case class Iteration(val start:Instant, val end:Instant){
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
      
      def next(i:Iteration) = Iteration(i.end, i.end.toDateTime.plusDays(14).toInstant());
      
      def iterationsStarting(day:Instant):Stream[Iteration] = {
              def loop(n:Iteration):Stream[Iteration] = {n#::loop(next(n))}
              loop(Iteration(day, day.toDateTime.plusDays(14).toInstant()))
      }
      def iterationsFromTeamInfo(team:TeamDto) = {
          val its = team.iterations
          its.zipWithIndex.flatMap{entry=>
          val (i, idx) = entry;
          
          println("Teams")
          
          if(idx==0){
              None
          }else{
              val prev = its(idx-1)
              Some(Iteration(start=new Instant(prev.start), end = new Instant(i.start)))
          }
          
          }
      }
      
      val allVersions = allVersionsOfBacklog(backlogId)
      
      def publishedVersions = allVersions.filter(_.backlog.memo!="wip")
      
      val iterationsThisYear = iterationsStarting(new YearMonthDay(2014, 1, 1).toDateMidnight().toInstant()).take(25)
//      val iterationsThisYear = iterationsFromTeamInfo(data.teams.toStream.head)
      
      println("Iterations: " + iterationsThisYear)
      
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
        
        val addedThisIteration = diff.added.totalPoints - diff.removed.totalPoints + diff.reopened.totalPoints + diff.reestimated.totalPoints
        
        val todo = d.startingState.backlog.todo - diff.finished.totalPoints
        
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
            finished=diff.finished.totalPoints,
            whenProjectedComplete=whenProjectedComplete) 
      }
      
      OK(Jackson.JerksonJson(iterationStats))
    }
    
}