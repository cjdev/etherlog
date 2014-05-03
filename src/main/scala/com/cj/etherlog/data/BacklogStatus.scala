package com.cj.etherlog.api

case class BacklogStatus (
    val id:String,
    val latestVersion:String,
    val whenArchived:Option[Long]
)