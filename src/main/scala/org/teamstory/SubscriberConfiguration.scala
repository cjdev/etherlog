package org.teamstory

import java.io.{File, IOException, FileNotFoundException}
import scala.io.Source
import scala.collection.mutable.{ListBuffer}

object SubscriberConfiguration {
    def getSubscriberUrls(fileName:String):Option[ListBuffer[String]] = {
        val pathToFile = new File(fileName)
        if(pathToFile.exists()) {
          val urls:ListBuffer[String] = new ListBuffer[String]()

          try {
            for(line <- Source.fromFile(fileName).getLines()) {
              println(line)
              if (line.length > 10) {

                urls.append(line)
              }
            }
          } catch {
            case ex: FileNotFoundException => println("Couldn't find that file.")
            case ex: IOException => println("Had an IOException trying to read that file")
          }

          Some(urls)
        }else{
          None
        }

    }
}

