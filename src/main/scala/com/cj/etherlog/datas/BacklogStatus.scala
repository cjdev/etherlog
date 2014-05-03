package com.cj.etherlog.datas

case class BacklogStatus (
    val id:String,
    val latestVersion:String,
    val whenArchived:Option[Long]
)