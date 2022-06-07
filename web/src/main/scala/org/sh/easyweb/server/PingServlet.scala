package org.sh.easyweb.server

import javax.servlet.http.HttpServlet
import javax.servlet.http.{HttpServletRequest => HReq}
import javax.servlet.http.{HttpServletResponse => HResp}

class PingServlet extends HttpServlet {
  override def doGet(hReq: HReq, hResp: HResp) = doPost(hReq, hResp)
  override def doPost(hReq: HReq, hResp: HResp) = {
    hResp.setContentType("text/plain")
    hResp.getWriter.print("pong")
  }
}
