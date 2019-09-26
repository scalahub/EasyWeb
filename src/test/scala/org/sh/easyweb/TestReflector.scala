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
  assert(a == $a$, s"Expected ${$a$} found $a")
  assert(b == $b$, s"Expected ${$b$} found $b")

  val method2 = fp.getPublicMethods(2)
  val expected2 = "def bar(boxName:String, value:long, ergoScript:org.sh.easyweb.Text, useP2S:boolean, registerKeys:String[], tokenIDs:byte[][], tokenAmts:long[]): void"
  val found2 = method2.toScalaString

  val method2InfoVars = method2.infoVars

  val expectedMap = Map(
    ("$useP2S$","false"),
    ("$tokenIDs$","[]"),
    ("$registerKeys$","[b,c]"),
    ("$value$","10000"),
    ("$boxName$","box1"),
    ("$ergoScript$","{\\n  val x = blake2b256(c)\\n  b == 1234.toBigInt &&\\n  c == x\\n}"),
    ("$tokenAmts$","[]"),
    ("$INFO$","If use P2S is false then it will use P2SH address")
  )

  method2InfoVars.foreach{
    case (key, value) =>
      val expected = expectedMap.get(key).getOrElse("None")
      assert(expected == value, s"Expected: $expected. Actual: $value")
  }

  assert(method2InfoVars.size == expectedMap.size, s"InfoVars size (${method2InfoVars.size}) != Expected size (${expectedMap.size})")
  println("INFOVARS passed")
  assert(found2 == expected2, s"\nExpected:\n   $expected2\nFound:\n   $found2")
  println("SIGNATURE passed")

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

  def bar(boxName:String, value:Long, ergoScript:Text, useP2S:Boolean, registerKeys:Array[String], tokenIDs:Array[Array[Byte]], tokenAmts:Array[Long]) = {
    val $INFO$ = "If use P2S is false then it will use P2SH address"
    val $boxName$ = "box1"
    val $useP2S$ = "false"
    val $value$ = "10000"
    val $ergoScript$ = """{
  val x = blake2b256(c)
  b == 1234.toBigInt &&
  c == x
}"""
    val $registerKeys$ = "[b,c]"
    val $tokenIDs$ = "[]"
    val $tokenAmts$ = "[]"
  }

}
