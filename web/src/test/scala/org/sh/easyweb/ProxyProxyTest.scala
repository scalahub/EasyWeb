package org.sh.easyweb.test

import org.sh.reflect.test._
import org.sh.reflect.web.client.WebQueryMaker
import org.sh.reflect.Proxy

// see CommonReflectSocket for a better test. For the below test, the WebQueryResponder needs to be running, which needs
// a servlet to run (more complex)
object ProxyProxyTest extends App {
  val pqm = new ProxyQueryMaker(new WebQueryMaker)
  println (pqm.makeQuery(Proxy.metaPid(ProxyProxyServer.pid), "getMethodsInScala", ""))
  println (pqm.makeQuery(Proxy.metaPid("mtgmc"), "getMethodsInScala", ""))
}

