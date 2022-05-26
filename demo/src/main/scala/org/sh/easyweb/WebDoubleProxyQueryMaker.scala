package org.sh.easyweb

import org.sh.easyweb.client.WebQueryMaker
import org.sh.reflect.{DefaultTypeHandler, EasyProxy, QueryMaker}
import org.sh.utils.json.JSONUtil
import org.sh.webserver.EmbeddedWebServer

class DoubleProxyQueryMaker(qm: QueryMaker) extends QueryMaker {
  def makeQuery(pid: String, reqName: String, reqData: String) =
    qm.makeQuery(
      DoubleProxyServer.pid,
      "getResponse",
      DoubleProxyServer.getProxyReqJSON(pid, reqName, reqData)
    )
  def isConnected = qm.isConnected
}
object DoubleProxyServer {
  val pid = "DoubleProxy"
  EasyProxy.addProcessor(pid, "__", this, DefaultTypeHandler, true)

  def __getResponse(pid: String, reqName: String, reqData: String) =
    EasyProxy.getResponse(pid, reqName, reqData)
  def getProxyReqJSON(pid: String, reqName: String, reqData: String) =
    JSONUtil.createJSONString(
      Array("pid", "reqName", "reqData"),
      Array(pid, reqName, reqData)
    )
}

object DummyObject {
  val pid = "dummyTest"
  def a_foo(s: String, i: Int) = s + i
  def a_bar(s: String, i: Long) = s + i
  def a_baz(s: String) = Array("A", "b")
}

case class TestVector(
    pid: String,
    reqName: String,
    reqData: String,
    expected: String
)

object WebDoubleProxyQueryMaker extends App {
  println("sdlikbfvjsdflkgvbjhsdjkflhbgvnsdkjfnhbvgjksdf")
  val server = new EmbeddedWebServer(
    8080,
    None,
    Array[String](),
    Seq(
      (
        "/" + HTMLConstants.postUrl,
        classOf[org.sh.easyweb.server.WebQueryResponder]
      )
    )
  )
  val pqm = new DoubleProxyQueryMaker(new WebQueryMaker)
  val testVectors = Seq(
    TestVector(
      pid = EasyProxy.metaPid(DoubleProxyServer.pid),
      reqName = "getMethodsInScala",
      reqData = "",
      expected =
        """["def getResponse(pid:String, reqName:String, reqData:String): String"]"""
    ),
    TestVector(
      pid = EasyProxy.metaPid("dummyTest"),
      reqName = "getMethodsInScala",
      reqData = "",
      expected =
        """["def foo(s:String, i:int): String","def bar(s:String, i:long): String","def baz(s:String): String[]"]"""
    )
  )

  TestDoubleProxyServer.main(Array[String]())
  // calling above method instead of calling below two, which are called in above method
  //  EasyProxy.addProcessor(DummyObject.pid, "a_", DummyObject, DefaultTypeHandler, false)
  //  EasyProxy.addProcessor("myObjectID", "my_", MyObject, DefaultTypeHandler, processSuperClass)

  testVectors.foreach {
    case TestVector(pid, reqName, reqData, expected) =>
      val actual = pqm.makeQuery(pid, reqName, reqData)
      assert(actual == expected, s"Expected: ${expected}. Actual: ${actual}")
  }
  new TestDoubleProxyQueryMaker(pqm)
  println("WebProxy tests passed")
  server.server.stop()
  System.exit(0)
}

class TestDoubleProxyQueryMaker(qm: QueryMaker) {
  val pqm = new DoubleProxyQueryMaker(qm)
  TestVectors.testVectors.foreach {
    case TestVector(pid, reqName, reqData, expected) =>
      val actual = pqm.makeQuery(pid, reqName, reqData)
      assert(actual == expected, s"Expected: $expected. Actual: $actual")
  }
  println("Proxy tests passed for pid: " + DoubleProxyServer.pid)
}

object TestVectors {
  val testVectors = Seq(
    TestVector(
      EasyProxy.metaPid(DoubleProxyServer.pid),
      "getMethodsInJava",
      "",
      """["String getResponse(String pid,String reqName,String reqData)"]"""
    ),
    TestVector(
      EasyProxy.metaPid(DummyObject.pid),
      "getMethodsInScala",
      "",
      """["def foo(s:String, i:int): String","def bar(s:String, i:long): String","def baz(s:String): String[]"]"""
    ),
    TestVector(
      EasyProxy.metaPid(DoubleProxyServer.pid),
      "getMethodInScala",
      "{'name':'getResponse'}",
      """["def getResponse(pid:String, reqName:String, reqData:String): String"]"""
    ),
    TestVector(
      EasyProxy.metaPid(DummyObject.pid),
      "getMethodsInJava",
      "",
      """["String foo(String s,int i)","String bar(String s,long i)","String[] baz(String s)"]"""
    ),
    TestVector(DummyObject.pid, "foo", "{'s':'hello', 'i':'3'}", """hello3"""),
    TestVector(
      DoubleProxyServer.pid,
      "getResponse",
      "{'pid':'" + DummyObject.pid + "','reqName':'foo','reqData':'{\\'s\\':\\'hello\\', \\'i\\':\\'3\\'}'}",
      """hello3"""
    ),
    TestVector(
      DoubleProxyServer.pid,
      "getResponse",
      s"""{'pid':'${DummyObject.pid}','reqName':'foo','reqData':'{\\'s\\':\\'hello\\', \\'i\\':\\'3\\'}'}""",
      """hello3"""
    ),
    TestVector(
      DoubleProxyServer.pid,
      "getResponse",
      DoubleProxyServer
        .getProxyReqJSON(DummyObject.pid, "foo", "{'s':'hello', 'i':'3'}"),
      """hello3"""
    ),
    TestVector(
      "myObjectIDMeta",
      "getMethodsInScala",
      "",
      """["def mainMethod(a:int, b:String): scala.math.BigInt","def otherMethod(myParam:String): void"]"""
    )
  )
}

// following object tests above DoubleProxyServer.
object TestDoubleProxyServer extends App {

  object MyObject {
    def my_mainMethod(a: Int, b: String) = BigInt(a)
    def my_otherMethod(myParam: String) = {} // returns Unit
    def someOtherMethod(a: Int, b: String) = BigInt(a)
  }
  val processSuperClass = false
  // add the object to the form processor
  EasyProxy.addProcessor(
    "myObjectID",
    "my_",
    MyObject,
    DefaultTypeHandler,
    processSuperClass
  )
  // process only methods starting with "my"
  // use the id "myObjectID" to refer to the object when using Proxy

  // invoke the my_mainMethod using the Proxy, but skip the prefix ("my_") when calling
  assert(
    EasyProxy.getResponse(
      "myObjectID",
      "mainMethod",
      "{'a':'1','b':'hello'}"
    ) == """"1""""
  )

  // following will also work because method name in original object starts with "my_"
  assert(
    EasyProxy.getResponse(
      "myObjectID",
      "otherMethod",
      "{'myParam':'hello'}"
    ) == """Ok"""
  )

  // get details for methods available for invocation; append "Meta" to object id to get meta object id.
  // meta object is the object that stores information about the main object
  println(EasyProxy.getResponse("myObjectIDMeta", "getMethodsInScala", ""))

  assert(
    EasyProxy.getResponse(
      "myObjectIDMeta",
      "getMethodsInScala",
      ""
    ) == """["def mainMethod(a:int, b:String): scala.math.BigInt","def otherMethod(myParam:String): void"]"""
  )

  // do we need to test the below line?
  EasyProxy.addProcessor(
    DummyObject.pid,
    "a_",
    DummyObject,
    DefaultTypeHandler,
    false
  )

  TestVectors.testVectors.foreach {
    case TestVector(pid, reqName, reqData, expected) =>
      val actual = EasyProxy.getResponse(pid, reqName, reqData)
      assert(actual == expected, s"Expected: $expected. Actual: $actual")
  }
  println("Tests passed for pid: " + DoubleProxyServer.pid)
}
