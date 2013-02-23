package com.cj.etherlog

class BacklogVersion(
    val when:Long = System.currentTimeMillis(),
    val isPublished:Boolean, 
    val backlog:Backlog
)