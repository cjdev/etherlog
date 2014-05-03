package com.cj.etherlog

object Util {
    def parseBoolean(value:String):Option[Boolean] = {
        if(value==null){
            None
        } else value.toLowerCase() match {
        case "false" => Some(false)
        case "true" => Some(true)
        case _ => throw new Exception("Not true|false: " + value)
        } 
    }
}