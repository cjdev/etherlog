package org.teamstory.datas

import org.teamstory.api.PivotalTrackerLink

case class BacklogStatus (
    val id:String,
    val latestVersion:String,
    val whenArchived:Option[Long],
    val pivotalTrackerLink:Option[PivotalTrackerLink]
)