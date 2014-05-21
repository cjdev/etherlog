package com.cj.etherlog.http
import org.httpobjects._
import org.httpobjects.DSL._
import com.cj.etherlog.Jackson
import com.cj.etherlog.api.BacklogStatusPatch
import com.cj.etherlog.Etherlog
import com.cj.etherlog.Jackson._
import com.cj.etherlog.datas.BacklogVersion
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormat
import org.joda.time.YearMonthDay
import com.cj.etherlog.datas.Data
import com.cj.etherlog.Clock
import org.joda.time.format.DateTimeFormatter
import org.joda.time.ReadablePartial
import org.joda.time.DateTime
import org.joda.time.ReadableInstant

class DeltaResource (data:Data, clock:Clock) extends HttpObject("/api/backlogs/{id}/deltas/{rangeSpec}"){
  override def get(req:Request) = {
      val id = req.path().valueFor("id")
      val backlog = data.backlogs.get(id)
      
      val rangeSpec = req.path().valueFor("rangeSpec")
      case class DateRange(from:ReadablePartial, to:ReadablePartial)
      val publishedChanges = data.filterBacklogHistory(id, {version=>version.backlog.memo != "work-in-progress"})
      
      
      case class Versions(from:Option[BacklogVersion], to:Option[BacklogVersion])
      
      def versionsInTimeSpan(from:ReadableInstant, to:ReadableInstant) = {
          val backlog = data.backlogs.get(id);
          
          def changesUpTo(date:ReadableInstant) = publishedChanges.filter{change=>
            val changeDate = new Instant(change.when)
            !changeDate.isAfter(date)
          }
          Versions(from=changesUpTo(from).headOption, 
                   to=changesUpTo(to).headOption )
      }
      
      def versionsInDateRange(range:DateRange) = {
          val backlog = data.backlogs.get(id);
          
          def changesUpTo(date:ReadablePartial) = publishedChanges.filter{change=>
            val changeDate = new Instant(change.when).toDateTime().toYearMonthDay()
            !changeDate.isAfter(date)
          }
          Versions(from=changesUpTo(range.from).headOption, 
                   to=changesUpTo(range.to).headOption )
      }
      
      def parseDateTime(string:String) = {
        val DateTimeString = "(....)-(..)-(..):(..)(..)".r
        string match {
          case DateTimeString(year, month, day, hour, minute)=>new DateTime(year.toInt, month.toInt, day.toInt, hour.toInt, minute.toInt, 0, 0)
          case _ => throw new Exception("Not a valid date+time string: " + string)
        }
      }
      
      val SinceLastPublished = "since-last-published".r
      val SinceDatePattern = "since-(....-..-..)".r
      val SinceDateTimePattern = "since-(....-..-..:....)".r
      val SinceIdPattern = """since-([0-9|a-z|\-]*)""".r
      val BetweenDatesPattern = "from-(....-..-..)-to-(....-..-..)".r
      val BetweenDateTimesPattern = "from-(....-..-..:....)-to-(....-..-..:....)".r
      val BetweenTimestampsPattern = "from-([0-9]*)-to-([0-9]*)".r
      val BetweenIdsPattern = """from-([0-9|a-z|\-]*)-to-([0-9|a-z|\-]*)""".r
      val InIdPattern = """in-([0-9|a-z|\-]*)""".r
      
      def millisToYMD(millis:Long) = new Instant(millis).toDateTime().toYearMonthDay()
      def millisStringToYMD(millis:String) = millisToYMD(millis.toLong)
      def parseDate(text:String) = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(text).toYearMonthDay()
      println(rangeSpec)
      val versions = rangeSpec match {
        case InIdPattern(toId) => {
          val to = Some(data.versions.get(toId))
          val from = publishedChanges.filter(_.when<to.get.when).headOption
          Versions(from=from, to=to)
        }
        case SinceDateTimePattern(string) => {
          val when = parseDateTime(string).toInstant()
          println("parsed " + when + " to " + clock.now)
          versionsInTimeSpan(from=when, to=clock.now)
        }
        case BetweenDateTimesPattern(fromString, toString) => {
          versionsInTimeSpan(from=parseDateTime(fromString), to=parseDateTime(toString))
          
        }
        case "since-last-published" => {
          println("since last published")
          Versions(from=Some(publishedChanges.head), to=Some(data.versions.get(backlog.latestVersion)))}
        case SinceDatePattern(date) => versionsInDateRange(DateRange(from=parseDate(date), to = new YearMonthDay(clock.now)))
        case BetweenDatesPattern(from, to) => versionsInDateRange(DateRange(from=parseDate(from), to=parseDate(to)))
        case BetweenTimestampsPattern(from, to) => versionsInDateRange(DateRange(from=millisStringToYMD(from), to=millisStringToYMD(to)))
        case BetweenIdsPattern(from, to) => {
          val foo = publishedChanges.find(_.id==from)
          val bar = publishedChanges.find(_.id==to)
          Versions(from=foo, to=bar)
        }
        case SinceIdPattern(from) => {
          Versions(from=publishedChanges.find(_.id==from), to=publishedChanges.headOption)
        }
      }
      
      versions match {
        case Versions(Some(from), Some(to))=> OK(JerksonJson(to.delta(from)))
        case Versions(_, _) => {
          NOT_FOUND(Text("Valid dates are " + millisToYMD(publishedChanges.last.when) + " to " + millisToYMD(publishedChanges.head.when)))
        }
      }
      
  }
}