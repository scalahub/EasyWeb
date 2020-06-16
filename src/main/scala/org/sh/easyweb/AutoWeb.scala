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
  val webDir = read("htmldir", "src/main/webapp") // previously was "autoweb"
  val fileNamePrefix = ""
  val prefix = ""
  FUtil.createDir(webDir)

  val webAppGenerator = new HTMLClientCodeGenerator(
    anyRefs,
    appInfo,
    None,
    false,
    false
  )

  webAppGenerator.autoGenerateToFile(
    fileNamePrefix, webDir, prefix
  )(ignoreMethodStr.map{
    case (x, y) => (y, x) // need to fix this. Why do we need to reverse?
  })

  val htmlFile = s"${fileNamePrefix}AutoGen.html"

  anyRefs.foreach(EasyProxy.addProcessor(prefix, _, DefaultTypeHandler, true))
  List("*Restricted*").foreach(EasyProxy.preventMethod)
  new EmbeddedWebServer(8080, None,
    Array(s"$webDir/$htmlFile"),
    Seq(
      ("/"+HTMLConstants.postUrl, classOf[org.sh.easyweb.server.WebQueryResponder]),
      ("/"+HTMLConstants.fileUploadUrl, classOf[org.sh.easyweb.server.FileUploaderNIO]),
      ("/"+HTMLConstants.fileDownloadUrl, classOf[org.sh.easyweb.server.FileDownloaderNIO])
    )
  )

  def generateWebXml = {
    val webXmlDir = s"$webDir/WEB-INF"
    val webXmlFile = s"$webXmlDir/web.xml"
    val webXmlFileText =
      s"""
        |<?xml version="1.0" encoding="ISO-8859-1" ?>
        |
        |<web-app
        |        xmlns="http://java.sun.com/xml/ns/javaee"
        |        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        |        xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
        |        version="3.0">
        |
        |    <display-name>AutoWeb</display-name>
        |    <description>AutoWeb application</description>
        |
        |    <servlet>
        |        <servlet-name>ProxyServlet</servlet-name>
        |        <servlet-class>org.sh.easyweb.server.WebQueryResponder</servlet-class>
        |    </servlet>
        |    <servlet>
        |        <servlet-name>UploadServlet</servlet-name>
        |        <servlet-class>org.sh.easyweb.server.FileUploaderNIO</servlet-class>
        |    </servlet>
        |    <servlet>
        |        <servlet-name>DownloadServlet</servlet-name>
        |        <servlet-class>org.sh.easyweb.server.FileDownloaderNIO</servlet-class>
        |    </servlet>
        |    <servlet-mapping>
        |        <servlet-name>ProxyServlet</servlet-name>
        |        <url-pattern>/${HTMLConstants.postUrl}</url-pattern>
        |    </servlet-mapping>
        |    <servlet-mapping>
        |        <servlet-name>UploadServlet</servlet-name>
        |        <url-pattern>/${HTMLConstants.fileUploadUrl}</url-pattern>
        |    </servlet-mapping>
        |    <servlet-mapping>
        |        <servlet-name>DownloadServlet</servlet-name>
        |        <url-pattern>/${HTMLConstants.fileDownloadUrl}</url-pattern>
        |    </servlet-mapping>
        |
        |    <welcome-file-list>
        |        <welcome-file>$htmlFile</welcome-file>
        |    </welcome-file-list>
        |</web-app>
        |""".stripMargin

    org.sh.utils.file.Util.createDir(webXmlDir)
    org.sh.utils.file.Util.writeToTextFile(webXmlFile, webXmlFileText)
  }

  //////////////////////////////////////////////////////
  // Open page in browser
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
