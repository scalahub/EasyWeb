name := "EasyWeb"

scalaVersion := "2.12.10"

ThisBuild / version := "1.0"

val commonDependencies =
  libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.4"

// contains autowired client / server and code gen utils
lazy val web = project
  .in(file("web"))
  .settings(
    commonDependencies,
    libraryDependencies += "io.github.scalahub" %% "easymirror" % "1.0"
  )

// below project contains the EmbeddedWebServer
lazy val webserver = project
  .in(file("webserver"))
  .settings(
    commonDependencies,
    libraryDependencies += "io.github.scalahub" %% "scalautils" % "1.0"
  )

lazy val root = project
//  .aggregate(webserver, web)
  .in(file("."))
  .dependsOn(
    webserver,
    web
  )

lazy val demo = (project in file("demo"))
  .dependsOn(
    root
  )
  .enablePlugins(JettyPlugin)
  .settings(
    Compile / run / mainClass := Some(
      "org.sh.easyweb.MyBasicDemo"
    ), // basic web page
    Compile / run / mainClass := Some(
      "org.sh.easyweb.WebDoubleProxyQueryMaker"
    ), // proxy-proxy server
    Compile / run / mainClass := Some(
      "org.sh.easyweb.MyAdvancedDemo"
    ) // various tests
  )
