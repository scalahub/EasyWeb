package org.sh.easyweb

import org.sh.easyweb.server.WebQueryResponder
import org.sh.reflect.{DefaultTypeHandler, FormProcessor}

object TestReflector extends App {

  WebQueryResponder // access class to add handling of Text type in methods
  val fp = new FormProcessor("", TestObj, DefaultTypeHandler, None, false)

  val $a$ = "Some\\nLarge Text\\nwith\\nmultiple\\nlines\\n"
  val $b$ = "hello"
  val $INFO$ = "method_info"

  val m = fp.getPublicMethods(0)
  val INFO = m.methodInfo.getOrElse("no info")
  val a = m.infoVars.get("$a$").getOrElse("noInfo")
  val b = m.infoVars.get("$b$").getOrElse("noInfo")

  assert(INFO == $INFO$, s"expected ${$INFO$}. Found $INFO")
  println("INFO VAR passed")
  assert(a == $a$, s"Expected ${$a$} found $a")
  assert(b == $b$, s"Expected ${$b$} found $b")
  println("PARA VARS passed")

}

object TestObj {
  def method(a:Int, b:String) = {
    val $INFO$ = "method_info"
    val $a$ = """Some
Large Text
with
multiple
lines
"""
    val $b$ = "hello"
  }
  def foo(a:Int) = {}
}
//*/