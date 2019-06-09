package org.sh.easyweb

import org.sh.easyweb.client.WebQueryMaker
import org.sh.reflect._
import org.sh.webserver.EmbeddedWebServer

object WebDoubleProxyQueryMaker extends App {
  val server = new EmbeddedWebServer(
    8080,
    None,
    Array[String](),
    Seq(("/"+HTMLConstants.postUrl, classOf[org.sh.easyweb.server.WebQueryResponder]))
  )
  val pqm = new DoubleProxyQueryMaker(new WebQueryMaker)
  val testVectors = Seq(
    TestVector(
      pid = EasyProxy.metaPid(DoubleProxyServer.pid),
      reqName = "getMethodsInScala",
      reqData = "",
      expected = """["def getResponse(pid:String, reqName:String, reqData:String): String"]"""
    ),
    TestVector(
      pid = EasyProxy.metaPid("dummyTest"),
      reqName = "getMethodsInScala",
      reqData = "",
      expected = """["def foo(s:String, i:int): String","def bar(s:String, i:long): String","def baz(s:String): String[]"]"""
    )
  )

  TestDoubleProxyServer.main(Array[String]())
  // calling above method instead of calling below two, which are called in above method
  //  EasyProxy.addProcessor(DummyObject.pid, "a_", DummyObject, DefaultTypeHandler, false)
  //  EasyProxy.addProcessor("myObjectID", "my_", MyObject, DefaultTypeHandler, processSuperClass)


  testVectors.foreach{
    case TestVector(pid, reqName, reqData,expected) =>
      val actual = pqm.makeQuery(pid, reqName, reqData)
      assert(actual == expected, s"Expected: ${expected}. Actual: ${actual}")
  }
  new TestDoubleProxyQueryMaker(pqm)
  println("WebProxy tests passed")
  server.server.stop()
  System.exit(0)
}

