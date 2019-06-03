package org.sh.easyweb.client

import org.sh.easyweb.server.WebQueryResponder
import org.sh.utils.common.encoding.Base64._
import org.sh.utils.common.file.TraitFilePropertyReader
import org.sh.utils.common.Util._
import org.sh.utils.common.curl.CurlAlt._
import org.sh.reflect.QueryMaker
import org.sh.utils.common.encoding.{Base64, Hex}

class WebQueryMaker(val servletUrl:String = "http://localHost:8080/web") extends QueryMaker with TraitFilePropertyReader {
  // not for browser but via direct java http request. 
  val propertyFile = "client.properties"
  val queryTimeOut = read("queryTimeOut", 1000)
  /**
   * The makeQuery method takes in a processor id (identifying the class that will process the request), a query name indicating the method to call and data for the method
   *
   * This method internally does the following:
   * encodes the information in string.
   * sends the packet to the server (which then forwards to some object)
   * waits for the response or TIMEOUT
   * returns whatever it received
   *
   * @param pid the processor id
   * @param queryName the name of method to invoke
   * @param queryData the data for the method (encoded in JSON)
   */
  var reqID = 0
  def getReqID = {
    reqID += 1
    reqID.toString
  }
  def getUrl(reqID:String, pid:String, reqName:String, reqData:String) = {
    val url = servletUrl+"?reqId="+reqID+"&pid="+pid+"&reqName="+reqName+"&reqData="+encodeBytes(reqData.getBytes)
    url
  }
  def makeQuery (pid:String, queryName:String, queryData:String) = {
    val reqID = getReqID
    val url = getUrl(getReqID, pid, queryName, queryData)
    val curled = curl(url, Array[(String, String)](), post,
      Array[(String, String)](
        ("reqId", reqID),
        ("pid", pid),
        ("reqName", queryName),
        ("reqData", encodeBytes(queryData.getBytes))
      )
    )
    val compressedStr = curled.split(":")(1)
    val str = WebQueryResponder.uncompress(compressedStr)
    str
  }
  def isConnected = true 
}
