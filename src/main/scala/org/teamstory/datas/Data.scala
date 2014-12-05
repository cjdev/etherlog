package org.teamstory.datas

import org.teamstory.api._
import org.teamstory.api.GlobalConfig
import org.teamstory.api.TeamDto
import org.teamstory.api.HistoryItem
import org.teamstory.api.BacklogListEntry
import org.teamstory.authenticate.Session
import org.teamstory.authenticate.User

trait Data {
    val teams: DatabaseTrait[TeamDto]
    val errors: DatabaseTrait[String]
    val backlogs: DatabaseTrait[BacklogStatus]
    val versions: DatabaseTrait[BacklogVersion]
    val sessions: DatabaseTrait[Session]
    val users: DatabaseTrait[User]
    val passwords: DatabaseTrait[String]

    def setGlobalConfig(n:GlobalConfig)
    def getGlobalConfig(): GlobalConfig

    def scanBacklogHistory(backlogId:String, fn:(BacklogVersion)=>Unit):Unit
    def filterBacklogHistory(backlogId: String, fn: (BacklogVersion) => Boolean): Seq[BacklogVersion]

    def getHistory(id: String): Seq[HistoryItem]
    def buildStatsLog(id: String, until: Long, includeCurrentState: Boolean = false):Seq[(StatsLogEntry, BacklogVersion)]
    
    def toBacklogListEntry(backlogStatus: BacklogStatus):BacklogListEntry

    val versionsCache: scala.collection.mutable.Map[String, BacklogVersion]
}