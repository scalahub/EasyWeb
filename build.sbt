name := "EasyWeb"

scalaVersion := "2.12.10"

val commonResolvers = resolvers ++= Seq(
  "SonaType Snapshots s01" at "https://s01.oss.sonatype.org/content/repositories/snapshots/"
)

val commonDependencies =
  libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.4"

// contains autowired client / server and code gen utils
lazy val web = project
  .in(file("web"))
  .settings(
    commonResolvers,
    commonDependencies,
    libraryDependencies += "io.github.scalahub" %% "easymirror" % "0.1.0-SNAPSHOT"
  )

// below project contains the EmbeddedWebServer
lazy val webserver = project
  .in(file("webserver"))
  .settings(
    commonResolvers,
    commonDependencies,
    libraryDependencies += "io.github.scalahub" %% "scalautils" % "0.1.0-SNAPSHOT"
  )

lazy val root = project
  .aggregate(webserver, web)
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
