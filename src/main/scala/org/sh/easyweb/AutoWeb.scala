package org.sh.easyweb

import org.sh.reflect.{DefaultTypeHandler, EasyProxy}
import org.sh.utils.common.Util
import org.sh.webserver.EmbeddedWebServer
import org.sh.utils.common.file.{TraitFilePropertyReader, Util => FUtil}

class AutoWeb(anyRefs:List[AnyRef], appInfo:String) extends TraitFilePropertyReader{
  override val propertyFile: String = "autoweb.properties"
  val htmldir = read("htmldir", "autoweb")
  //val fileNamePrefix = Util.randomAlphanumericString(50)
  val fileNamePrefix = ""
  val prefix = ""

  val dataDir = "dataDir"
  FUtil.createDir(dataDir)
  val h = new HTMLClientCodeGenerator(
    anyRefs,
    "/web",
    appInfo,
    None,
    false,
    false
  )
  h.autoGenerateToFile(
    fileNamePrefix, dataDir, prefix
  )

  anyRefs.foreach(EasyProxy.addProcessor(prefix, _, DefaultTypeHandler, true))
  List("*Restricted*").foreach(EasyProxy.preventMethod)
  new EmbeddedWebServer(8080, None,
    Array(s"$dataDir/${fileNamePrefix}AutoGen.html"),
    Seq(
      ("/web", classOf[org.sh.easyweb.server.WebQueryResponder]),
      ("/putfile", classOf[org.sh.easyweb.server.FileUploaderNIO]),
      ("/getfile", classOf[org.sh.easyweb.server.FileDownloaderNIO])
    )
  )

  //////////////////////////////////////////////////////
  // from https://stackoverflow.com/a/18509384/243233
  import java.awt.Desktop
  import java.net.URI

  val url = "http://localhost:8080"
  if (Desktop.isDesktopSupported) {
    val desktop = Desktop.getDesktop
    desktop.browse(new URI(url))
  } else {
    val runtime = Runtime.getRuntime
    runtime.exec("xdg-open " + url)
  }
  //////////////////////////////////////////////////////
}
