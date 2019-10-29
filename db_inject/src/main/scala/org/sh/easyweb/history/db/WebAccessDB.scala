package org.sh.easyweb.history.db

import org.sh.db.config.DBConfigFromFile
import org.sh.db.core.DataStructures._
import org.sh.db.{DBManager => DBMgr}
import org.sh.utils.Util._
import org.sh.utils.file.TraitFilePropertyReader
import org.sh.utils.json.JSONUtil
import org.sh.utils.json.JSONUtil.JsonFormatted
import org.sh.easyweb.WebDataStructures._
import org.sh.db.ScalaDB._
import org.sh.easyweb.history.WebAccessHistory
import org.sh.easyweb.server.WebQueryResponder

object WebAccessDB extends TraitFilePropertyReader {
  val propertyFile = "webAccess.properties"
  var webLoggingMode = read("webLoggingMode", true) // log queries by default
  val logResponses = read("logResponses", false) // dont log responses by default
  implicit val privOrdDBConfig = new DBConfigFromFile("webAccessDB.properties")  

  val (reqCol, respCol, timeCol) = (Col("req", BLOB), Col("resp", BLOB), Col("time", ULONG))
  
  val STR = VARCHAR(255)
  val srcCol = Col("src", STR)

  val internalIDCol = Col("internalReqID", STR) // an internal req/resp id randomly generated
  val externalIDCol = Col("externalReqID", STR) // a requestID send from client
  val methodNameCol = Col("methodName", STR)
  

  val accessCols = Array(internalIDCol, externalIDCol, methodNameCol, reqCol, respCol, timeCol, srcCol)
  val accessDB = Tab.withName("webAccessDB").withCols(accessCols).withPriKey()

  def toBytes(a:Any) = a.toString.getBytes("UTF-16")
  def fromBytes(a:Array[Byte]) = new String(a, "UTF-16")
  val adderPID = org.sh.reflect.Util.getDefaultPid(WebAccessHistory)
  def addToDB(req:Req, resp:Resp, src:Option[String]) = {      
    // toDo. add limit to responses
    if (webLoggingMode && okToLog(req)) accessDB.insert(
      getID,
      req.reqID,
      req.reqName,
      toBytes(req), toBytes(if (logResponses) resp else "not logged"), getTime, src.getOrElse("none")
    )
  }
  val forbiddenKeyWords = Array("seed", "password", "secret", "hex")
  def reqHasForbiddenKeys(req:Req) = {
    req.getReqDataKeys.exists{key =>
      val lower = key.toLowerCase
      forbiddenKeyWords.exists{fk =>
        lower.contains(fk)
        // lower.startsWith(fk) || lower.endsWith(fk)
      }
    }
  }
  def okToLog(req:Req) = !notOkToLog(req)
  def notOkToLog(req:Req) = (req.pid == adderPID || reqHasForbiddenKeys(req))
  case class AccessSummary(req:Array[Byte], time:Long, src:String, internalID:String) extends JsonFormatted {
    val jsonString = fromBytes(req)
    val reqKeys = JSONUtil.getJSONKeys(jsonString).filterNot(_ == "reqData")
    
    val dataJsonString = JSONUtil.getJSONParams(List("reqData"), jsonString)(0)
    val dataKeys = JSONUtil.getJSONKeys(dataJsonString)
    
    val keys = Array("time", "reqSrc", "internalID") ++ reqKeys ++ dataKeys.map("data_"+_)
    val vals:Array[Any] = Array(toDateString(time), src, internalID) ++ JSONUtil.getJSONParams(reqKeys, jsonString) ++ JSONUtil.getJSONParams(dataKeys, dataJsonString) 
  }
  def toAccess(a:Array[Any]) = AccessSummary(a(0).as[Array[Byte]], a(1).as[Long], a(2).as[String], a(3).as[String])
  
  WebQueryResponder.addToOnReqResp("webProxy", WebAccessDB.addToDB)
  
}  

