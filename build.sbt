name := "EasyWeb"

scalaVersion := "2.12.10"

ThisBuild / version := "1.2"

lazy val commonDependencies =
  libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.4"

lazy val commonResolvers = resolvers ++= Seq(
  "Sonatype Releases" at "https://s01.oss.sonatype.org/content/repositories/releases",
  "Sonatype Releases 2" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "SonaType Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/",
  "SonaType Staging" at "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
)

// contains autowired client / server and code gen utils
lazy val web = project
  .in(file("web"))
  .settings(
    commonDependencies,
    commonResolvers,
    libraryDependencies += "io.github.scalahub" %% "easymirror" % "1.1"
  )

// below project contains the EmbeddedWebServer
lazy val webserver = project
  .in(file("webserver"))
  .settings(
    commonDependencies,
    commonResolvers,
    libraryDependencies += "io.github.scalahub" %% "scalautils" % "1.0"
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
