package com.cj.etherlog

import org.codehaus.jackson.annotate.JsonTypeInfo
import org.codehaus.jackson.annotate.JsonTypeInfo.Id
import org.codehaus.jackson.annotate.JsonTypeInfo.As

case class Estimate(
    val id:String,
    val currency:String,
    val value:Int,
    val when:Long
    )
    
case class Item(
    val id:String,
    val isComplete:Option[Boolean],
    val name:String,
    val kind:String,
    val estimates:Option[List[Estimate]]
)

case class Backlog (
    val id:String,
    val name:String,
    val memo:String,
    val items:List[Item])