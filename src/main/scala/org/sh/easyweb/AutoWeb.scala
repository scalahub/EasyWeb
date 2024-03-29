package org.sh.easyweb

import org.sh.reflect.{CodeGenUtil, DefaultTypeHandler, EasyProxy}
import org.sh.utils.file.{Util => FUtil}
import org.sh.webserver.EmbeddedWebServer

/**
  *
  * @param anyRefs A list of objects or class instances for which HTML is to be generated
  * @param appInfo Some string (can contain HTML) describhing the application
  * @param ignoreMethodStr A list of methods to be ignored, given as list of pairs of type: ("class_path", "methodName").
  *                        For instance, to ignore methods called "bar" and "baz" in an "Foo" within package "org.my.app",
  *                        use List(("org.my.app.Foo", "bar"), ("org.my.app.Foo", "baz")). By default, this is an empty list.
  */
object AutoWeb {
  val webDir = "src/main/webapp" // previously was "autoweb"
  val srcDir = "src/main/scala"
  val fileNamePrefix = ""
  val htmlFile = s"${fileNamePrefix}AutoGen.html"
  val prefix = ""
}

class AutoWeb(
    anyRefs: List[AnyRef],
    appInfo: String,
    ignoreMethodStr: List[(String, String)] = Nil
) {
  import AutoWeb._
  FUtil.createDir(webDir)

  val webAppGenerator = new HTMLClientCodeGenerator(
    anyRefs,
    appInfo,
    None,
    false,
    false
  )

  webAppGenerator.autoGenerateToFile(
    fileNamePrefix,
    webDir,
    prefix
  )(ignoreMethodStr.map {
    case (x, y) => (y, x) // need to fix this. Why do we need to reverse?
  })

  anyRefs.foreach(EasyProxy.addProcessor(prefix, _, DefaultTypeHandler, true))
  List("*Restricted*").foreach(EasyProxy.preventMethod)
  new EmbeddedWebServer(
    8080,
    None,
    Array(s"$webDir/$htmlFile"),
    Seq(
      (
        "/" + HTMLConstants.postUrl,
        classOf[org.sh.easyweb.server.WebQueryResponder]
      ),
      (
        "/" + HTMLConstants.fileUploadUrl,
        classOf[org.sh.easyweb.server.FileUploaderNIO]
      ),
      (
        "/" + HTMLConstants.fileDownloadUrl,
        classOf[org.sh.easyweb.server.FileDownloaderNIO]
      )
    )
  )

  case class BetterString(s: String) {
    def clean = if (s.endsWith("$")) s.init else s
  }
  implicit def stringToBetterString(s: String) = new BetterString(s)

  def generateInitServlet: AutoWeb = {
    val scalaSrcDir = s"$srcDir/easyweb"
    val scalaFile = s"$scalaSrcDir/Initializer.scala"
    println(s"Writing Scala code to $scalaFile")
    val scalaFileText =
      s"""
        |/*
        |${CodeGenUtil.preamble(this)}
        |*/
        |package easyweb {
        |  import javax.servlet.http.HttpServlet
        |  import javax.servlet.http.{HttpServletRequest => HReq}
        |  import javax.servlet.http.{HttpServletResponse => HResp}
        |  import org.sh.reflect.{DefaultTypeHandler, EasyProxy}
        |
        |
        |  class Initializer extends HttpServlet {
        |
        |    val anyRefs = List(
        |      ${anyRefs
        .map(_.getClass.getCanonicalName.clean)
        .reduceLeft(_ + "," + _)}
        |    )
        |    anyRefs.foreach(EasyProxy.addProcessor("$prefix", _, DefaultTypeHandler, true))
        |    def getReq(hReq:HReq) = {}
        |    override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
        |    override def doPost(hReq:HReq, hResp:HResp) = {}
        |  }
        |}
        |""".stripMargin

    org.sh.utils.file.Util.createDir(scalaSrcDir)
    org.sh.utils.file.Util.writeToTextFile(scalaFile, scalaFileText)
    this
  }

  def generateWebXml = {
    val webXmlDir = s"$webDir/WEB-INF"
    val webXmlFile = s"$webXmlDir/web.xml"
    val webXmlFileText =
      s"""
        |<!--
        |${CodeGenUtil.preamble(this)}
        |-->
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
        |        <servlet-name>InitializerServlet</servlet-name>
        |        <servlet-class>easyweb.Initializer</servlet-class>
        |        <load-on-startup>1</load-on-startup>
        |    </servlet>
        |    <servlet>
        |        <servlet-name>QueryServlet</servlet-name>
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
        |    <servlet>
        |        <servlet-name>PingServlet</servlet-name>
        |        <servlet-class>org.sh.easyweb.server.PingServlet</servlet-class>
        |    </servlet>
        |    <servlet-mapping>
        |        <servlet-name>QueryServlet</servlet-name>
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
        |    <servlet-mapping>
        |        <servlet-name>PingServlet</servlet-name>
        |        <url-pattern>/ping</url-pattern>
        |    </servlet-mapping>
        |
        |    <welcome-file-list>
        |        <welcome-file>$htmlFile</welcome-file>
        |    </welcome-file-list>
        |</web-app>
        |""".stripMargin

    org.sh.utils.file.Util.createDir(webXmlDir)
    org.sh.utils.file.Util.writeToTextFile(webXmlFile, webXmlFileText)
    generateInitServlet
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
