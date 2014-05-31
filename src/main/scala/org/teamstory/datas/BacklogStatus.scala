package org.teamstory.datas

case class BacklogStatus (
    val id:String,
    val latestVersion:String,
    val whenArchived:Option[Long]
)