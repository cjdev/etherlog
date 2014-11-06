import com.fasterxml.jackson.annotation.JsonIgnore
import org.joda.time.YearMonthDay

package org.teamstory.api {

case class GlobalConfig(defaultChartType: String = "default", clockOffset: Long = 0)

case class IterationDto(start: Long, label: String)

case class TeamDto(id: String, name: String, iterations: Seq[IterationDto])

case class HistoryItem(version: String, when: Long, memo: String)

case class StatsLogEntry(version: String, when: Long, memo: String, team: Int, nonTeam: Int, done: Int) {
  def todo = team + nonTeam
  def total = done + todo
}

case class BacklogListEntry(id: String, name: String, whenArchived: Option[Long])

case class PivotalTrackerLink(projectId:Int, apiKey:String)

case class BacklogStatusPatch(archived: Option[Boolean], pivotalTrackerLink:Option[PivotalTrackerLink])

case class VersionNameAndTime(
        id: String,
        memo: String,
        when: Long)

case class ItemIdAndShortName(id: String, name: String)

case class ItemGroupSynopsis(items: Seq[ItemIdAndShortName], totalPoints: Int)

case class Delta(from: VersionNameAndTime, to: VersionNameAndTime,
        added: ItemGroupSynopsis, removed: ItemGroupSynopsis, finished: ItemGroupSynopsis, reopened: ItemGroupSynopsis, reestimated: ItemGroupSynopsis)

case class Estimate(
        val id: String,
        val currency: String,
        val value: Int,
        val when: Long
        )

case class Item(
        val id: String,
        val isComplete: Option[Boolean] = Some(false),
        val inProgress: Option[Boolean] = Some(false),
        val name: String,
        val kind: String,
        val estimates: Option[Seq[Estimate]],
        val when: Option[Long] = None
        ) {

  @JsonIgnore
  def isStoryOrEpic() = kind == "epic" || kind == "story"

  def shortName = {
    val firstLine = name.lines.toList.headOption.getOrElse("")
    val maxLength = 100
    if (firstLine.length() > maxLength) firstLine.substring(0, maxLength) + "..."
    else firstLine
  }
  def hasCompleted = isComplete.getOrElse(false)
  def isInProgress = inProgress.getOrElse(false)
  def toIdAndShortName = ItemIdAndShortName(id, shortName)

  def bestEstimate() = estimates match {
    case Some(e) => {
      val latestEstimate = e.maxBy(_.when)
      if (latestEstimate != null) {
        Some(latestEstimate.value);
      } else {
        None
      }
    }
    case None => None
  }
}

case class BacklogCalculations(
        val pointsDone:Int, 
        val pointsTodo:Int, 
        val pointsInProgress:Int)

case class BacklogDto(
        val id: String,
        val name: String,
        val memo: String,
        val projectedVelocity: Option[Int] = None,
        val items: Seq[Item],
        val optimisticLockVersion: Option[String] = None,
        val calculations:BacklogCalculations)

}
