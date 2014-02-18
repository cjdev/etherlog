package com.cj.etherlog

import org.httpobjects.jetty.HttpObjectsJettyHandler
import org.httpobjects.HttpObject
import org.httpobjects.DSL._
import org.httpobjects.Request
import org.httpobjects.util.ClasspathResourcesObject
import org.httpobjects.util.ClasspathResourceObject
import java.io.InputStream
import java.io.InputStreamReader
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import org.httpobjects.Representation
import java.io.OutputStream
import java.io.{File => Path}
import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.StringBuilder
import java.io.FileInputStream
import org.joda.time.DateMidnight
import org.joda.time.Instant
import com.cj.etherlog.api._
import com.cj.etherlog.chart._
import org.joda.time.Months
import org.joda.time.Weeks
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.HttpClient
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat
import org.joda.time.YearMonthDay

object Etherlog {
  
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
  
  val jackson:ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper
  }
  
  def parseJson[T](is:InputStream)(implicit manifest:Manifest[T]):T = {
    jackson.readValue[T](is, manifest.erasure.asInstanceOf[Class[T]])
  }
  
  def JerksonJson(o:AnyRef) = {
    new Representation(){
      override def contentType = "application/json"
      override def write(out:OutputStream){
        jackson.writeValue(out, o)
      }
    }
  }
  
  class Database[T](basePath:Path){
    basePath.mkdirs();
    
    def put(id:String, data:T):Unit  = this.synchronized{
      jackson.writeValue(pathFor(id), data)
    }
    
    def get(id:String)(implicit manifest:Manifest[T]):T = this.synchronized {
        val stream = new FileInputStream(pathFor(id))
        try {
            parseJson(stream)
        }finally{
          stream.close();
        }
        
    }
    
    def scan(fn:(String, T)=>Unit)(implicit manifest:Manifest[T]) {
      val files = basePath.listFiles()
      if(files!=null){
        files.foreach{file=>
          val id = file.getName()
          val value = get(id)
          fn(id, value)
        }
      }
    }
    
    def contains(id:String) = pathFor(id).exists()
    
    private def pathFor(id:String) = new Path(basePath, id);
  }
  
  
  def main(args: Array[String]) {
    val showLatestEvenIfWipParamName = "showLatestEvenIfWip"
    
    val dataPath = new Path("data");
    
    val errors = new Database[String](new Path(dataPath, "errors"))
    val backlogs = new Database[BacklogStatus](new Path(dataPath, "backlogs"))
    val versions = new Database[BacklogVersion](new Path(dataPath, "versions"))
    
    
    
    val versionsCache = scala.collection.mutable.Map[String, BacklogVersion]()
    
    def scanBacklogHistory(backlogId:String, fn:(BacklogVersion)=>Unit):Unit = {
      val backlog = backlogs.get(backlogId);
      var nextVersionId = backlog.latestVersion
      while(nextVersionId!=null){
        val version = versionsCache.get(nextVersionId) match {
          case Some(version)=>version
          case None=> versions.get(nextVersionId)
        }
        versionsCache.put(nextVersionId, version)
        fn(version)
        nextVersionId = version.previousVersion
      }
     }
    
    def filterBacklogHistory(backlogId:String, fn:(BacklogVersion)=>Boolean):Seq[BacklogVersion]  = {
      var results = ListBuffer[BacklogVersion]()
      
      scanBacklogHistory(backlogId, {next=>
          val matches = fn(next)
          if(matches) results += next
      })
      
      results.toSeq
    }
    
    def buildStatsLog(id:String, until:Long, includeCurrentState:Boolean = false)= {
      val backlog = backlogs.get(id) 
              val allResults = new ListBuffer[(StatsLogEntry, BacklogVersion)]()
              
              def incrementedEstimate(tally:Int, item:Item) = {
                  item.bestEstimate match {
                  case Some(value) => {
                      value + tally
                  }
                  case None => tally
                  }
              }
              
              scanBacklogHistory(id, {version=>
                    val items = version.backlog.items
                    val amountComplete:Int = items.filter(_.isComplete.getOrElse(false)).foldLeft(0)(incrementedEstimate);
                    val amountTodo:Int = items.filter(!_.isComplete.getOrElse(false)).foldLeft(0)(incrementedEstimate);
                    
                    val n = (StatsLogEntry(
                            version=version.id, 
                            when=version.when, 
                            memo=version.backlog.memo,
                            todo=amountTodo,
                            done=amountComplete), version)
                    
                    allResults += n
                                
              }) 
              
              var results = allResults.filter(_._1.when<=until)
              
              var (latest, _) = results.head
              
              results.filter{next=>
                    var (item, _) = next
                    var includeBecauseItsLast = (includeCurrentState && item.version == latest.version)
                    var includeBecauseItsNotWIP = item.memo!="work-in-progress" 
                    includeBecauseItsLast || includeBecauseItsNotWIP
              }
              
    }
    
     class ChartStylesheet(parentPath:String) extends HttpObject(parentPath + "/mystyle.css"){
            override def get(req:Request) = {
              OK(FromClasspath("text/css", "/content/mystyle.css", getClass))
            }
        }
     
     def updateBacklog(newBacklogState:Backlog) = {
       
          val backlogId = this.synchronized{
            var candidate = 1
            while(backlogs.contains(candidate.toString())){
              candidate += 1;
            }
            candidate.toString()
          }
          
          val initialBacklog = Backlog(
              id=backlogId, 
              name=newBacklogState.name, 
              memo=newBacklogState.memo, 
              items = newBacklogState.items)
          val initialVersion = BacklogVersion(
                  id=UUID.randomUUID().toString(),
                  backlog = initialBacklog,
                  isPublished= false, 
                  previousVersion = null
          )
          
          versions.put(initialVersion.id, initialVersion)
          
          val status = BacklogStatus(
              id=backlogId,
              latestVersion = initialVersion.id
              )
          
          backlogs.put(backlogId, status)
          status.id
     }
    
     def parseBoolean(value:String):Option[Boolean] = {
       if(value==null){
         None
       } else value.toLowerCase() match {
         case "false" => Some(false)
         case "true" => Some(true)
         case _ => throw new Exception("Not true|false: " + value)
       } 
     }
     
     def buildStatsLogFromQueryString(id:String, req:Request) = {
      val endParam = req.query().valueFor("end")
      val showLatestEvenIfWipParam = req.query().valueFor(showLatestEvenIfWipParamName)
      
      val end = if(endParam==null) System.currentTimeMillis() else endParam.toLong
      val showLatestEvenIfWip = parseBoolean(showLatestEvenIfWipParam).getOrElse(false)
      
      
      buildStatsLog(id=id, until=end, includeCurrentState = showLatestEvenIfWip);
      
     }
     
    def getHistory(id:String):Seq[HistoryItem] = {
      val results = new ListBuffer[HistoryItem]()
      scanBacklogHistory(id, {version=>
        results += HistoryItem(version=version.id, when=version.when, memo=version.backlog.memo)
      }) 
      results.toSeq
    }
     
    
    val port = 43180
    
    HttpObjectsJettyHandler.launchServer(port, 
        new HttpObject("/api/backlogs"){
            override def get(req:Request) = {
               val results = new ListBuffer[BacklogListEntry]()
               
               backlogs.scan{(id, backlog)=>
                 val version = versions.get(backlog.latestVersion)
                 results += BacklogListEntry(id=id, name=version.backlog.name)
               }
              
               OK(JerksonJson(results))
            }
            
            override def post(req:Request) = {
              val backlogRecieved = parseJson[Backlog](readAsStream(req.representation()));
              
              val newVersionId = updateBacklog(backlogRecieved);
              
              CREATED(Location("/api/backlogs/" + newVersionId))
            }
        },
        new ChartStylesheet("/api/backlogs/{id}"),
        new ChartStylesheet("/backlog/"),
        new HttpObject("/api/backlogs/{id}/chart"){
            override def get(req:Request) = {
              val id = req.path().valueFor("id")
              val stats = buildStatsLogFromQueryString(id, req);
              val nowParam = req.query().valueFor("now");
              val now = if(nowParam==null) System.currentTimeMillis() else nowParam.toLong
              
              val lastTime = now + (Months.months(3).toMutablePeriod().toDurationFrom(new Instant(now)).getMillis())
//              println(new Instant(lastTime))
              
              val version = stats.head._2
              
              val text = makeSvg(
                              stats=stats.map(_._1).toList.reverse, 
                              lastTime = lastTime, 
                              whenProjectedComplete = version.projectedEnd.getOrElse(0),
                              goals=version.backlog.goalData
                         )
              
              OK(Bytes("image/svg+xml", text.getBytes()))
            }
            
        },
        new HttpObject("/api/backlogs/{id}/history"){
            override def get(req:Request) = {
              val id = req.path().valueFor("id")
              val showLatestEvenIfWipParam = req.query().valueFor(showLatestEvenIfWipParamName)
              
              val showLatestEvenIfWip = parseBoolean(showLatestEvenIfWipParam).getOrElse(false)
              
              val fullHistory = getHistory(id)
              
              val results = fullHistory.filter{item=>
                    val isLast = fullHistory.head eq item
                    (showLatestEvenIfWip && isLast) || item.memo != "work-in-progress"
              }
              OK(JerksonJson(results))
            }
            
        },
        new HttpObject("/api/backlogs/{id}/statsLog"){
            override def get(req:Request) = {
              val id = req.path().valueFor("id")
              val stats = buildStatsLogFromQueryString(id, req);
              
              OK(JerksonJson(stats))
            }
        },
        new HttpObject("/api/backlogs/{id}/history/{version}"){
            override def get(req:Request) = {
              val id = req.path().valueFor("id")
              val versionId = req.path().valueFor("version")
              val results = new ListBuffer[HistoryItem]()
              
              if(versions.contains(versionId)){
                val version = versions.get(versionId);
                  
                OK(JerksonJson(version.backlog))
              }else{
                NOT_FOUND(Text("No such version"))
              }
            }
            
        },
        new HttpObject("/api/backlogs/{id}"){
            override def get(req:Request) = {
              val id = req.path().valueFor("id")
              val backlog = backlogs.get(id)
              val currentVersion = versions.get(backlog.latestVersion);
              OK(JerksonJson(currentVersion.backlog))
            }
            override def put(req:Request) = {
              val id = req.path().valueFor("id")
              val newBacklog = parseJson[Backlog](readAsStream(req.representation()));
              val backlog = backlogs.get(id);
              
              val newVersion = new BacklogVersion(
                                  id = UUID.randomUUID().toString(),
                                  when = System.currentTimeMillis(),
                                  isPublished = false,
                                  previousVersion = backlog.latestVersion,
                                  backlog = newBacklog
                              )
              val updatedBacklog = new BacklogStatus(
                                          backlog.id, 
                                          latestVersion = newVersion.id)
              
              versions.put(updatedBacklog.latestVersion, newVersion);
              backlogs.put(id, updatedBacklog)

                notifySubscribers(backlog)
              get(req)
            }
        },
        new HttpObject("/api/errors"){
          
            override def post(req:Request) = {
              val errorId = UUID.randomUUID().toString();
              val error = asString(readAsStream(req.representation()))
              
              errors.put(errorId, error)
              
              CREATED(Location("/api/errors/" + errorId))
            }
        },
        new HttpObject("/api/backlogs/{id}/deltas"){
          
            override def get(req:Request) = {
              val id = req.path().valueFor("id")
              
              def toLongOr(s:String, default:Long) = if(s==null) default else s.toLong
              
              val from = toLongOr(req.query().valueFor("from"), 0)
              val to = toLongOr(req.query().valueFor("to"), new Instant().getMillis())
              val backlog = backlogs.get(id);
              
              val changes = filterBacklogHistory(id, {version=>
                (version.backlog.memo != "work-in-progress") && 
                (version.when > from) && 
                (version.when<=to)
              }).reverse
              
              val deltas = changes.zipWithIndex.flatMap{item=>
                val (version, idx) = item;
                if(idx > 0){
                  val previous = changes(idx-1)
                  Some(version.delta(previous))
                }else{
                  None
                }
              }
              
              OK(JerksonJson(deltas))
            }
        },
        new HttpObject("/api/backlogs/{id}/deltas/{rangeSpec}"){
          override def get(req:Request) = {
              val id = req.path().valueFor("id")
              
              val rangeSpec = req.path().valueFor("rangeSpec")
              
              case class DateRange(from:YearMonthDay, to:YearMonthDay)
              val publishedChanges = filterBacklogHistory(id, {_.backlog.memo != "work-in-progress"})
              
              case class Versions(from:Option[BacklogVersion], to:Option[BacklogVersion])
              
              def versionsInDateRange(range:DateRange) = {
                  val backlog = backlogs.get(id);
                  
                  def changesUpTo(date:YearMonthDay) = publishedChanges.filter{change=>
                    val changeDate = new Instant(change.when).toDateTime().toYearMonthDay()
                    !changeDate.isAfter(date)
                  }
                  Versions(from=changesUpTo(range.from).headOption, 
                           to=changesUpTo(range.to).headOption )
              }
              
              val SinceDatePattern = "since-(.*)".r
              val BetweenDatesPattern = "from-(....-..-..)-to-(....-..-..)".r
              val BetweenTimestampsPattern = "from-([0-9]*)-to-([0-9]*)".r
              val BetweenIdsPattern = """from-([0-9|a-z|\-]*)-to-([0-9|a-z|\-]*)""".r
              
              def millisToYMD(millis:Long) = new Instant(millis).toDateTime().toYearMonthDay()
              def millisStringToYMD(millis:String) = millisToYMD(millis.toLong)
              def parseDate(text:String) = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(text).toYearMonthDay()
              
              val versions = rangeSpec match {
                case SinceDatePattern(date) => versionsInDateRange(DateRange(from=parseDate(date), to = new YearMonthDay()))
                case BetweenDatesPattern(from, to) => versionsInDateRange(DateRange(from=parseDate(from), to=parseDate(to)))
                case BetweenTimestampsPattern(from, to) => versionsInDateRange(DateRange(from=millisStringToYMD(from), to=millisStringToYMD(to)))
                case BetweenIdsPattern(from, to) => {
                  val foo = publishedChanges.find(_.id==from)
                  val bar = publishedChanges.find(_.id==to)
                  Versions(from=foo, to=bar)
                }
              }
              
              versions match {
                case Versions(Some(from), Some(to))=> OK(JerksonJson(to.delta(from)))
                case Versions(_, _) => {
                  NOT_FOUND(Text("Valid dates are " + millisToYMD(publishedChanges.last.when) + " to " + millisToYMD(publishedChanges.head.when)))
                }
              }
              
          }
        },
        new ClasspathResourceObject("/mockup", "/content/backlog-mockup.html", getClass()),
        new ClasspathResourceObject("/", "/content/index.html", getClass()),
        new ClasspathResourceObject("/backlog/{backlogId}", "/content/backlog.html", getClass()),
        new ClasspathResourcesObject("/{resource*}", getClass(), "/content")
    );
    
    println("etherlog is alive and listening on port " + port);
  }

    def notifySubscribers(backlog: BacklogStatus) {
        val subscribers:ListBuffer[String] = SubscriberConfiguration.getSubscriberUrls("subscribers.properties")
        subscribers.foreach(url => {
            val subscriberUrl = url + backlog.id
            val postMethod = new PostMethod(subscriberUrl)
            val client = new HttpClient()
            val response = client.executeMethod(postMethod)

            if (response != 200) {
                println("NO RESPONSE FROM BOARD")
            } else {
                println("Board notified id " + backlog.id)
            }
        })
    }
}