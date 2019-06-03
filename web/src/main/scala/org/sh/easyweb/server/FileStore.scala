// used for uploading / downloading files

package org.sh.easyweb.server

import org.sh.utils.common.file.TraitFilePropertyReader
import org.sh.utils.common.file.Util._
import org.sh.utils.common.Util._
import org.sh.utils.common.json.JSONUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import org.sh.reflect.DefaultTypeHandler

// NOTE: This is only for storing temp files. DO NOT USE FOR App specific files.
// The only purpose of the file store is for giving a File upload and download link for java.io.File types in the HTML UI

@deprecated object FileStore extends TraitFilePropertyReader {
  val propertyFile = "fileStore.properties"
  
  val uploadDir = read("uploadDir", ".")+"/org.sh.reflect_uploads"
  val maxSizeBytes = read("maxSizeBytes", 10000000) // 10 MB
  val maxRetainTime = read("maxRetainTimeHours", 24L) * OneHour
  val isNioMode = read("isNioMode", true)
  val isPlayMode = read("isPlayMode", false)
  
  createDir(uploadDir)
  def putFile(file:File):String = {
    val (tmpFile, fileID) = putNewFileAndGetID(Some(file.getName))
    Files.copy(file.toPath, tmpFile.toPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)    
    fileID
  }
  def time(file:File) = {
    Files.readAttributes(file.toPath, classOf[BasicFileAttributes]).lastModifiedTime.toMillis
  }
  def getFile(fileID:String):File = {
    val saveDir = new File(uploadDir, shaSmall(fileID))
    if (!saveDir.isDirectory) throw new Exception("invalid fileID") 
    val files = saveDir.listFiles    
    val actualFile = if (files.size == 1) files(0) else files.sortBy(time).head
    if (actualFile.isFile) actualFile else throw new Exception("invalid fileID (not a file)") 
  }
  def putNewFileAndGetID(implicit fileName:Option[String] = None) = {
    val fileID = org.sh.utils.common.Util.randomAlphanumericString(20)
    val saveDir = new File(uploadDir, shaSmall(fileID))    
    if (!saveDir.mkdir) throw new Exception("could not create upload dir")    
    val name = if (fileName.isDefined) fileName.get else org.sh.utils.common.Util.randomAlphanumericString(20)
    val file = new File(saveDir.getAbsolutePath, name)
    (file, fileID)
  }  
  
  //  val isPlayMode = true
  val jettyLink = "?fileID="
  val playLink = "/"
  val actualLink = if (isPlayMode) playLink else jettyLink
    
  
  def getFileLink(file:File) = {
    val fileID = putFile(file)
    //    <a target="_blank" href={"/getfile?fileID="+fileID}>{fileID}</a>.toString
    val html = s"""<a target='_blank' href='/getfile$actualLink$fileID'>$fileID</a>"""
    html
  }
  def getFileLinks(files:Array[File]) = JSONUtil.encodeJSONArray(files.map(getFileLink)).toString
  def getFileLinks(files:Iterable[File]) = JSONUtil.encodeJSONArray(files.toArray.map(getFileLink)).toString

  /*
  // below code for deleting files. Commented out for now
  doEvery30Mins{
    tryIt{
      val retainAfter = getTime - maxRetainTime
      val files = org.sh.utils.common.file.Util.getAllFiles(uploadDir, null, true).map(new File(_)).filter{f =>
        f.lastModified < retainAfter && f.isFile
      } // extension null returns all files
      files.foreach{f =>
        tryIt(f.delete)
        tryIt(f.getParentFile.delete)
      }
      // http://www.avajava.com/tutorials/lessons/how-do-i-get-all-files-with-certain-extensions-in-a-directory-including-subdirectories.html
      // http://www.mkyong.com/java/how-to-get-the-file-last-modified-date-in-java/
    }
  }
  */
  println("initializing file store")
  
}
