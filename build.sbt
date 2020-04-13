
name := "EasyWeb"

version := "0.1"

lazy val ScalaUtils = RootProject(uri("https://github.com/scalahub/ScalaUtils.git"))
lazy val EasyMirror = RootProject(uri("https://github.com/scalahub/EasyMirror.git"))

//lazy val ScalaUtils = RootProject(uri("../ScalaUtils"))
//lazy val EasyMirror = RootProject(uri("../EasyMirror"))

libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.4"

lazy val web = (project in file("web")).dependsOn(
  EasyMirror % "compile->compile;test->test"
)

// below project contains the EmbeddedWebServer
lazy val webserver = (project in file("webserver")).dependsOn(ScalaUtils)

lazy val root = (project in file(".")).dependsOn(
  webserver, web
)

lazy val demo = (project in file("demo")).dependsOn(
  root,
  EasyMirror % "compile->compile;test->test"
).enablePlugins(JettyPlugin).settings(
  mainClass in (Compile, run) := Some("org.sh.easyweb.MyBasicDemo"),
  mainClass in (Test, run) := Some("org.sh.easyweb.WebDoubleProxyQueryMaker")
)
