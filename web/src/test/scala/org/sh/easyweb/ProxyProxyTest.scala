package org.sh.easyweb

import org.sh.easyweb.client.WebQueryMaker
import org.sh.reflect.{EasyProxy, ProxyProxyServer, ProxyQueryMaker}

// see CommonReflectSocket for a better test. For the below test, the WebQueryResponder needs to be running, which needs
// a servlet to run (more complex)
object ProxyProxyTest extends App {
  val pqm = new ProxyQueryMaker(new WebQueryMaker)
  println (pqm.makeQuery(EasyProxy.metaPid(ProxyProxyServer.pid), "getMethodsInScala", ""))
  println (pqm.makeQuery(EasyProxy.metaPid("testpid"), "getMethodsInScala", ""))
}

