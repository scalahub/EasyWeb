package org.sh.easyweb

import java.io.File
import java.nio.file.Files

import javax.servlet.http.HttpServlet
//import jdk.internal.net.http.ResponseBodyHandlers.FileDownloadBodyHandler
import org.sh.easyweb.server.{FileStoreNIO, FileUploaderNIO}
import org.sh.reflect.{DefaultTypeHandler, EasyProxy}
import org.sh.webserver.EmbeddedWebServer

object MyConfig {
  val myFirstClass = new MyFirstClass
  val mySecondClass = new MySecondClass
  val listObj = List(
    MyFirstObject,
    MySecondObject,
    myFirstClass
  )  // objects to be generated in html
  val prefix = "myMethod_" // only methods starting with "myMethod_" will be considered
  val fileNamePrefix = "MyHtml" // html file will have "MyHtml" prepended
  val dir = "demo/src/main/webapp" // directory where HTML will be generated
}

import MyConfig._

object MyHTMLGen extends App {
  val h = new HTMLClientCodeGenerator(listObj, "", None, false, false)
  h.c :+= mySecondClass // how to add later
  h.autoGenerateToFile(
    fileNamePrefix, dir, prefix
  )
}

object MyWebServer extends App {
  MyHTMLGen.main(Array[String]()) // generate html
  MyProxy // start proxy
  new EmbeddedWebServer(8080, None,
    Array(s"{$dir}/{$fileNamePrefix}AutoGen.html"),
    Seq(
      ("/"+HTMLConstants.postUrl, classOf[org.sh.easyweb.server.WebQueryResponder]),
      ("/init", classOf[org.sh.easyweb.MyServlet]),
      ("/"+HTMLConstants.fileUploadUrl, classOf[org.sh.easyweb.server.FileUploaderNIO]),
      ("/"+HTMLConstants.fileDownloadUrl, classOf[org.sh.easyweb.server.FileDownloaderNIO])
    )
  )

  //////////////////////////////////////////////////////
  // from https://stackoverflow.com/a/18509384/243233
  import java.awt.Desktop
  import java.io.IOException
  import java.net.URI
  import java.net.URISyntaxException

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

class MyServlet extends HttpServlet {
  MyProxy // refer to initialize object to start EasyProxy server
}

object MyProxy {
  import MyConfig._
  listObj.foreach(EasyProxy.addProcessor(prefix, _, DefaultTypeHandler, true))
  List(mySecondClass).foreach(EasyProxy.addProcessor(prefix, _, DefaultTypeHandler, true))
  List("*Restricted*").foreach(EasyProxy.preventMethod)
  DefaultTypeHandler.addType[MyType](classOf[MyType], string => new MyType(string), myType => myType.toString)
}
