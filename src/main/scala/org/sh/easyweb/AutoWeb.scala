package org.sh.easyweb

import org.sh.reflect.{DefaultTypeHandler, EasyProxy}
import org.sh.utils.Util
import org.sh.webserver.EmbeddedWebServer
import org.sh.utils.file.{TraitFilePropertyReader, Util => FUtil}

/**
  *
  * @param anyRefs A list of objects or class instances for which HTML is to be generated
  * @param appInfo Some string (can contain HTML) describhing the application
  * @param ignoreMethodStr A list of methods to be ignored, given as list of pairs of type: ("class_path", "methodName").
  *                        For instance, to ignore methods called "bar" and "baz" in an "Foo" within package "org.my.app",
  *                        use List(("org.my.app.Foo", "bar"), ("org.my.app.Foo", "baz")). By default, this is an empty list.
  */
class AutoWeb(anyRefs:List[AnyRef], appInfo:String, ignoreMethodStr:List[(String, String)] = Nil) extends TraitFilePropertyReader{
  override val propertyFile: String = "autoweb.properties"
  val htmldir = read("htmldir", "autoweb")
  val fileNamePrefix = ""
  val prefix = ""
  FUtil.createDir(htmldir)

  val h = new HTMLClientCodeGenerator(
    anyRefs,
    appInfo,
    None,
    false,
    false
  )
  h.autoGenerateToFile(
    fileNamePrefix, htmldir, prefix
  )(ignoreMethodStr.map{
    case (x, y) => (y, x) // need to fix this. Why do we need to reverse?
  })

  anyRefs.foreach(EasyProxy.addProcessor(prefix, _, DefaultTypeHandler, true))
  List("*Restricted*").foreach(EasyProxy.preventMethod)
  new EmbeddedWebServer(8080, None,
    Array(s"$htmldir/${fileNamePrefix}AutoGen.html"),
    Seq(
      ("/"+HTMLConstants.postUrl, classOf[org.sh.easyweb.server.WebQueryResponder]),
      ("/"+HTMLConstants.fileUploadUrl, classOf[org.sh.easyweb.server.FileUploaderNIO]),
      ("/"+HTMLConstants.fileDownloadUrl, classOf[org.sh.easyweb.server.FileDownloaderNIO])
    )
  )

  /*
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
  */
}
