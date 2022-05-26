name := "auto_web"

scalaVersion := "2.12.10"

val commonResolvers = resolvers ++= Seq(
  "SonaType Snapshots s01" at "https://s01.oss.sonatype.org/content/repositories/snapshots/"
)

val commonDependencies =
  libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.4"

// contains autowired client / server and code gen utils
lazy val easy_web = project
  .in(file("web"))
  .settings(
    commonResolvers,
    commonDependencies,
    libraryDependencies += "io.github.scalahub" %% "easy_mirror" % "0.1.0-SNAPSHOT" % "compile->compile;test->tests"
  )

// below project contains the EmbeddedWebServer
lazy val web_server = project
  .in(file("webserver"))
  .settings(
    commonResolvers,
    commonDependencies,
    libraryDependencies += "io.github.scalahub" %% "scalautils" % "0.1.0-SNAPSHOT"
  )

lazy val root = project
  .in(file("."))
  .dependsOn(
    web_server,
    easy_web
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
