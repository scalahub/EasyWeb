package org.sh.easyweb

import org.sh.reflect.CodeGenUtil
import org.sh.utils.file.{Util => FUtil}

class AutoWebSession(anyRefs:List[AnyRef], appInfo:String, ignoreMethodStr:List[(String, String)] = Nil) {
  import AutoWeb._
  FUtil.createDir(webDir)

  case class BetterString(s:String) {
    def clean = if (s.endsWith("$")) s.init else s
  }
  implicit def stringToBetterString(s:String) = new BetterString(s)

  def generateInitServlet = {
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
         |  import org.sh.easyweb.{HTMLClientCodeGenerator, Random}
         |  import org.sh.reflect.{DefaultTypeHandler, EasyProxy}
         |
         |  class ShowHtmlServlet extends HttpServlet {
         |    val anyRefs = List(
         |      ${anyRefs.map(_.getClass.getCanonicalName.clean).reduceLeft(_ + "," + _)}
         |    )
         |    val htmlGen = new HTMLClientCodeGenerator(anyRefs, "${appInfo}", None, false, false)
         |    val html = htmlGen.generateFilteredOut("${prefix}", Nil)
         |    def isLocalHost(req:HReq) = req.getServerName == "localhost"
         |    override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
         |    override def doPost(hReq:HReq, hResp:HResp) = {
         |      val urlPattern = hReq.getPathInfo
         |      val isNewPatternNeeded = urlPattern == null || urlPattern.replace("/", "") == ""
         |      val isHttpsNeeded = hReq.getScheme == "http" && !isLocalHost(hReq)
         |      val secret = if (isNewPatternNeeded) "/"+Random.randString else urlPattern
         |
         |      if (isNewPatternNeeded || isHttpsNeeded) {
         |        hResp.sendRedirect(fullUrl(hReq, secret))
         |      } else {
         |        hResp.getWriter.print(html.replace("replaceWithActualSecret", secret))
         |      }
         |    }
         |    def getRedirectUrl(sessionUrl:String) = "/session"+sessionUrl
         |    def fullUrl(req:HReq, sessionUrl:String):String = {
         |      val relativeUrl = getRedirectUrl(sessionUrl)
         |      if (isLocalHost(req)) relativeUrl else {
         |        "https://"+req.getServerName+relativeUrl
         |      }
         |    }
         |  }
         |
         |  class Initializer extends HttpServlet {
         |
         |    val anyRefs = List(
         |      ${anyRefs.map(_.getClass.getCanonicalName.clean).reduceLeft(_+","+_)}
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
         |        <servlet-name>ShowHtmlServlet</servlet-name>
         |        <servlet-class>easyweb.ShowHtmlServlet</servlet-class>
         |    </servlet>
         |    <servlet>
         |        <servlet-name>QueryServlet</servlet-name>
         |        <servlet-class>org.sh.easyweb.server.WebQuerySessionResponder</servlet-class>
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
         |        <servlet-name>QueryServlet</servlet-name>
         |        <url-pattern>/${HTMLConstants.postUrl}</url-pattern>
         |    </servlet-mapping>
         |    <servlet-mapping>
         |        <servlet-name>UploadServlet</servlet-name>
         |        <url-pattern>/${HTMLConstants.fileUploadUrl}</url-pattern>
         |    </servlet-mapping>
         |    <servlet-mapping>
         |        <servlet-name>ShowHtmlServlet</servlet-name>
         |        <url-pattern>/session/*</url-pattern>
         |        <url-pattern>/session</url-pattern>
         |        <url-pattern></url-pattern>
         |    </servlet-mapping>
         |
         |    <servlet-mapping>
         |        <servlet-name>DownloadServlet</servlet-name>
         |        <url-pattern>/${HTMLConstants.fileDownloadUrl}</url-pattern>
         |    </servlet-mapping>
         |
         |</web-app>
         |""".stripMargin

    org.sh.utils.file.Util.createDir(webXmlDir)
    org.sh.utils.file.Util.writeToTextFile(webXmlFile, webXmlFileText)
    generateInitServlet
  }
}

