package org.sh.easyweb

import java.io.File
import java.nio.file.Files

import javax.servlet.http.HttpServlet
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
  val h = new HTMLClientCodeGenerator(listObj, "/web", "", None, false, false)
  h.c :+= mySecondClass // how to add later
  h.autoGenerateToFile(
    fileNamePrefix, dir, prefix
  )
}

object MyWebServer extends App {
  MyHTMLGen // generate html
  MyProxy // start proxy
  new EmbeddedWebServer(8080, None,
    Array("demo/src/main/webapp/MyHtmlAutoGen.html"),
    Seq(
      ("/web", classOf[org.sh.easyweb.server.WebQueryResponder]),
      ("/init", classOf[org.sh.easyweb.MyServlet]),
      ("/putfile", classOf[org.sh.easyweb.server.FileUploaderNIO]),
      ("/getfile", classOf[org.sh.easyweb.server.FileDownloaderNIO])
    )
  )
}

class MyServlet extends HttpServlet {
  MyProxy // refer to initialize object to start EasyProxy server
}

class MyType(val string:String) {
  override def toString = s"MyType($string)"
}

object MyProxy {
  import MyConfig._
  listObj.foreach(EasyProxy.addProcessor(prefix, _, DefaultTypeHandler, true))
  List(mySecondClass).foreach(EasyProxy.addProcessor(prefix, _, DefaultTypeHandler, true))
  List("*Restricted").foreach(EasyProxy.preventMethod)
  DefaultTypeHandler.addType[MyType](classOf[MyType], string => new MyType(string), myType => myType.toString)
}

class MyFirstClass {
  // following tests the HTML generated
  def myMethod_InfoVars(choice:Boolean, value:Int) = {
    val $INFO$ = "Info about someMethod. Check that choice is 'false' and value is '29'"
    val $choice$ = "false" // should set default choice radio button to "no"
    val $value$ = "29" // should set default value to "29"
    value.toString
  }
  def myMethod_Arrays(a:Array[Int], b:Array[String], c:Array[Boolean], d:Array[Byte], e:Array[Array[Byte]]) = {
    val s = a.sum + b.map(_.length).sum
    val r = c.foldLeft(true)((x, y) => x && y)
    if (r) s else -s
  }
  def otherMethod(choice:Boolean, arrFiles:Array[File]) = {
    // should not appear
    "Ok"
  }
}

class MySecondClass extends MyFirstClass {
  def myMethod_FileUpload(file:File) = {
    val $INFO$ = "File upload test. Should print the size of file"
    val $file$ = "This box will contain a file id after upload"
    file.length()
  }
  def myMethod_MyType(myType:MyType) = {
    new MyType(myType.string.reverse)
  }
}

object MyFirstObject {
  def myMethod_Choice(choice:Boolean) = {
    "Ok"
  }
  def myMethod_FileDownload(file:File) = {
    val $INFO$ = "Should return the same file back"
    file
  }
}

object MySecondObject extends MySecondClass {
  def myMethod_TextBox(text:Text) = {
    val $INFO$ = "Accepts text box"
    s"Text of ${text.getText.size} chars"
  }
  def myMethod_Restricted(text:Text) = {
    val $INFO$ = "Accepts text box"
    s"Text of ${text.getText.size} chars"
  }
}

