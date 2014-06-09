package org.teamstory.datas

import org.teamstory.api._
import org.teamstory.api.GlobalConfig
import org.teamstory.api.TeamDto
import org.teamstory.api.HistoryItem
import org.teamstory.api.StatsLogEntry

class DataStub extends Data{
  val teams: DatabaseTrait[TeamDto] = null
  val errors: DatabaseTrait[String] = null
  val backlogs: DatabaseTrait[BacklogStatus] = null
  val versions: DatabaseTrait[BacklogVersion] = null

  def setGlobalConfig(n:GlobalConfig) = ()
  def getGlobalConfig(): GlobalConfig = null

  def scanBacklogHistory(backlogId:String, fn:(BacklogVersion)=>Unit):Unit = ()
  def filterBacklogHistory(backlogId: String, fn: (BacklogVersion) => Boolean): Seq[BacklogVersion] = null

  def getHistory(id: String): Seq[HistoryItem] = null
  def buildStatsLog(id: String, until: Long, includeCurrentState: Boolean = false):Seq[(StatsLogEntry, BacklogVersion)] = null

  def toBacklogListEntry(backlogStatus: BacklogStatus):BacklogListEntry = null

  val versionsCache: scala.collection.mutable.Map[String, BacklogVersion] = null
}
