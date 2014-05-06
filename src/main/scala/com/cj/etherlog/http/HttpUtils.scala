package com.cj.etherlog.http

import org.httpobjects._
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.InputStream
import com.cj.etherlog.datas.Data
import com.cj.etherlog.Util
import com.cj.etherlog.Clock

object HttpUtils {
  val showLatestEvenIfWipParamName = "showLatestEvenIfWip"
      
  def readAsStream(r:Representation) = {
    val bytes = new ByteArrayOutputStream()
    r.write(bytes);
    bytes.close()
    new ByteArrayInputStream(bytes.toByteArray())
  }
  
  def asString(is:InputStream):String = {
    val reader = new InputStreamReader(is)
    val buffer = new Array[Char](33)
    val text = new StringBuilder()
    
    var numRead = reader.read(buffer);
    while(numRead>=0){
       text.append(buffer, 0, numRead)
       numRead = reader.read(buffer);
    }
    text.toString()
  }
  def buildStatsLogFromQueryString(id:String, req:Request, data:Data, clock:Clock) = {
    val endParam = req.query().valueFor("end")
    val showLatestEvenIfWipParam = req.query().valueFor(showLatestEvenIfWipParamName)
    val end = if(endParam==null) clock.now.getMillis else endParam.toLong
    val showLatestEvenIfWip = Util.parseBoolean(showLatestEvenIfWipParam).getOrElse(false)
    data.buildStatsLog(id=id, until=end, includeCurrentState = showLatestEvenIfWip);
  }

}