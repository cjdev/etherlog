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
){
  def bestEstimate() = estimates match {
      case Some(e) => {
          val latestEstimate = e.maxBy(_.when)
          if(latestEstimate!=null){
            Some(latestEstimate.value);
          }else{
            None
          }
      }
      case None => None
    }
}

case class Backlog (
    val id:String,
    val name:String,
    val memo:String,
    val items:List[Item])