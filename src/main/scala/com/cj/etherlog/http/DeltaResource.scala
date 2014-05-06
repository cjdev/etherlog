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

class DeltaResource (data:Data, clock:Clock) extends HttpObject("/api/backlogs/{id}/deltas/{rangeSpec}"){
  override def get(req:Request) = {
      val id = req.path().valueFor("id")
      
      val rangeSpec = req.path().valueFor("rangeSpec")
      
      case class DateRange(from:YearMonthDay, to:YearMonthDay)
      val publishedChanges = data.filterBacklogHistory(id, {_.backlog.memo != "work-in-progress"})
      
      case class Versions(from:Option[BacklogVersion], to:Option[BacklogVersion])
      
      def versionsInDateRange(range:DateRange) = {
          val backlog = data.backlogs.get(id);
          
          def changesUpTo(date:YearMonthDay) = publishedChanges.filter{change=>
            val changeDate = new Instant(change.when).toDateTime().toYearMonthDay()
            !changeDate.isAfter(date)
          }
          Versions(from=changesUpTo(range.from).headOption, 
                   to=changesUpTo(range.to).headOption )
      }
      
      val SinceDatePattern = "since-(....-..-..)".r
      val SinceIdPattern = """since-([0-9|a-z|\-]*)""".r
      val BetweenDatesPattern = "from-(....-..-..)-to-(....-..-..)".r
      val BetweenTimestampsPattern = "from-([0-9]*)-to-([0-9]*)".r
      val BetweenIdsPattern = """from-([0-9|a-z|\-]*)-to-([0-9|a-z|\-]*)""".r
      
      def millisToYMD(millis:Long) = new Instant(millis).toDateTime().toYearMonthDay()
      def millisStringToYMD(millis:String) = millisToYMD(millis.toLong)
      def parseDate(text:String) = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(text).toYearMonthDay()
      
      val versions = rangeSpec match {
        case SinceDatePattern(date) => versionsInDateRange(DateRange(from=parseDate(date), to = new YearMonthDay(clock.now)))
        case BetweenDatesPattern(from, to) => versionsInDateRange(DateRange(from=parseDate(from), to=parseDate(to)))
        case BetweenTimestampsPattern(from, to) => versionsInDateRange(DateRange(from=millisStringToYMD(from), to=millisStringToYMD(to)))
        case BetweenIdsPattern(from, to) => {
          val foo = publishedChanges.find(_.id==from)
          val bar = publishedChanges.find(_.id==to)
          Versions(from=foo, to=bar)
        }
        case SinceIdPattern(from) => {
          Versions(from=publishedChanges.find(_.id==from), publishedChanges.headOption)
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