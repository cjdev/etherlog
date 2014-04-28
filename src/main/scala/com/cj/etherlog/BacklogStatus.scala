package com.cj.etherlog

case class BacklogStatus (
    val id:String,
    val latestVersion:String,
    val whenArchived:Option[Long]
)