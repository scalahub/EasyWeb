
name := "EasyWeb"

version := "0.1"

scalaVersion := "2.12.8"

lazy val ScalaUtils = RootProject(uri("https://github.com/scalahub/ScalaUtils.git"))
//lazy val ScalaUtils = RootProject(uri("../ScalaUtils"))

lazy val EasyMirror = RootProject(uri("https://github.com/scalahub/EasyMirror.git"))
//lazy val EasyMirror = RootProject(uri("../EasyMirror"))

lazy val BetterDB = RootProject(uri("https://github.com/scalahub/BetterDB.git"))
//lazy val BetterDB = RootProject(uri("../BetterDB"))

lazy val web = (project in file("web")).dependsOn(
  EasyMirror % "compile->compile;test->test"
)

// below project contains the EmbeddedWebServer
lazy val webserver = (project in file("webserver")).dependsOn(ScalaUtils)

lazy val db_inject = (project in file("db_inject")).dependsOn(web, BetterDB)

lazy val root = (project in file(".")).dependsOn(
  db_inject,
  webserver
)

lazy val demo = (project in file("demo")).dependsOn(
  root,
  EasyMirror % "compile->compile;test->test"
).enablePlugins(JettyPlugin).settings(
  // Alternative main class, commented out. To run, use runMain
  // mainClass in (Compile, run) := Some("org.sh.easyweb.MyHTMLGen")
  mainClass in (Compile, run) := Some("org.sh.easyweb.MyWebServer"),
  mainClass in (Test, run) := Some("org.sh.easyweb.WebProxyQueryMaker")
)
