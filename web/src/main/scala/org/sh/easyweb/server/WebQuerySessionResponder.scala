
package org.sh.easyweb.server

import javax.servlet.http.{HttpServlet, HttpServletRequest => HReq, HttpServletResponse => HResp}

class WebQuerySessionResponder extends HttpServlet {
  import WebQueryResponder._
  override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
  override def doPost(hReq:HReq, hResp:HResp) = {
    val secret: Option[String] = Option(hReq.getParameter("secret")).map(_.replace("/", "")+"1")
    hResp.setContentType("text/plain")
    hResp.getWriter.write(getResp(getReq(hReq))(Some(hReq.getRemoteHost), secret))
  }
}
