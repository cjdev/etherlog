package com.cj.etherlog

case class BacklogVersion(
    val id:String,
    val when:Long = System.currentTimeMillis(),
    val isPublished:Boolean, 
    val previousVersion:String,
    val backlog:Backlog
)