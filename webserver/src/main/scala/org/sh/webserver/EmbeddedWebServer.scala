
package org.sh.webserver

import javax.servlet.http.HttpServlet

object EmbeddedWebServerTypes {
  type Route = String // route should be something like "/xquery"
  type ServletClass = Class[_ <: HttpServlet]
  type HttpPort = Int
  type HttpsPort = Int
  type KeyStoreFileName = String
  type KeyStorePassword = String
  type HttpsOptions = (HttpsPort, KeyStoreFileName, KeyStorePassword)
}
import EmbeddedWebServerTypes._

class EmbeddedWebServer(
  httpPort:HttpPort, 
  httpsOptions:Option[HttpsOptions],
  routesServlets:Seq[(Route, ServletClass)] //routesServlets:Seq[(String, Class[_ <: HttpServlet])]  
) {  
  def this(httpPort:Int, routesServlets:(Route, ServletClass)*) = this(httpPort, None, routesServlets.toSeq)
  def this(routesServlets:(Route, ServletClass)*) = this(8082, None, routesServlets.toSeq)
  
  import org.apache.http.HttpVersion
  import org.eclipse.jetty.server.HttpConfiguration
  import org.eclipse.jetty.server.HttpConnectionFactory
  import org.eclipse.jetty.server.SecureRequestCustomizer
  import org.eclipse.jetty.server.Server;
  import org.eclipse.jetty.server.ServerConnector
  import org.eclipse.jetty.server.SslConnectionFactory
  import org.eclipse.jetty.servlet.ServletHandler
  import org.eclipse.jetty.util.ssl.SslContextFactory

    /**
     * The simplest possible Jetty server.
     */
  val server = new Server(httpPort);
  val handler = new ServletHandler;
  server.setHandler(handler);
  routesServlets.foreach{
    case (route, myServletClass) => handler.addServletWithMapping(myServletClass, route);
  }
  
  val http_config = new HttpConfiguration();

  http_config.setOutputBufferSize(32768);
  http_config.setRequestHeaderSize(8192);
  http_config.setResponseHeaderSize(8192);
  http_config.setSendServerVersion(true);
  http_config.setSendDateHeader(false);

  httpsOptions.map{
    case (httpsPort, keyStoreFileName, keyStorePassword) =>
      http_config.setSecureScheme("https");
      http_config.setSecurePort(httpsPort);
        // === jetty-https.xml ===
        // SSL Context Factory
      val sslContextFactory = new SslContextFactory();
      sslContextFactory.setKeyStorePath(keyStoreFileName) // keystore.jks
      sslContextFactory.setKeyStorePassword(keyStorePassword) // ldwcniefrjnrfjvnr
      sslContextFactory.setKeyManagerPassword(keyStorePassword);
      sslContextFactory.setTrustStorePath(keyStoreFileName);
      sslContextFactory.setTrustStorePassword(keyStorePassword);
      sslContextFactory.setExcludeCipherSuites(
        "SSL_RSA_WITH_DES_CBC_SHA",
        "SSL_DHE_RSA_WITH_DES_CBC_SHA", 
        "SSL_DHE_DSS_WITH_DES_CBC_SHA",
        "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
        "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA"
      );

            // SSL HTTP Configuration
      val https_config = new HttpConfiguration(http_config);
      https_config.addCustomizer(new SecureRequestCustomizer());

            // SSL Connector
      val sslConnector = new ServerConnector(server,
      new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString),
      new HttpConnectionFactory(https_config));
      sslConnector.setPort(httpsPort);
      server.addConnector(sslConnector);  
  }
  server.start;
  server.dumpStdErr;
  org.sh.utils.common.Util.doOnceNow{server.join}
  
}
