
package org.sh.easyweb.history

import org.sh.easyweb.history.db.WebAccessDB._
import org.sh.db.core.DataStructures._

import org.sh.db.ScalaDB._
import org.sh.utils.Util._

object WebAccessHistory {

  def getAccessLog(from:Long, to:Long, max:Int,offset:Long) =
    accessDB.select(
      reqCol, timeCol, srcCol, internalIDCol
    ).where(
      timeCol >= from,
      timeCol <= to
    ).max(max).offset(offset).orderBy(timeCol.decreasing).as(toAccess)

  def purgeAccessLog(olderThanDays:Int, password:String) = {
    if (password != "wnw4uzmq29sw3du$2wz13") throw new Exception("invalid password")
    if (olderThanDays < 7) throw new Exception("Min days is 7")
    val millis = OneDay * olderThanDays
    val toTime = getTime - millis
    accessDB.deleteWhere(timeCol <= toTime)
  }

  def getResponse(internalID:String) =
    accessDB.select(respCol).where(internalIDCol === internalID).firstAs(_.as[Array[Byte]]).map{fromBytes}.headOption

  def getAccessLogForMethod(methodName:String, from:Long, to:Long, max:Int,offset:Long) =
    accessDB.select(
      reqCol, timeCol, srcCol, internalIDCol
    ).where(
      methodNameCol === methodName,
      timeCol >= from,
      timeCol <= to
    ).max(max).offset(offset).orderBy(timeCol.decreasing).as(toAccess)

}
