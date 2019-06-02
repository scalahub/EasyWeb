
name := "EasyWeb"

version := "0.1"

scalaVersion := "2.12.8"

lazy val EasyMirror = RootProject(uri("https://github.com/scalahub/EasyMirror.git"))
//lazy val EasyMirror = RootProject(uri("../EasyMirror"))

lazy val BetterDB = RootProject(uri("https://github.com/scalahub/BetterDB.git"))
//lazy val BetterDB = RootProject(uri("../BetterDB"))

lazy val web = (project in file("web")).dependsOn(
  EasyMirror % "compile->compile;test->test"
)

lazy val db_inject = (project in file("db_inject")).dependsOn(web, BetterDB)

lazy val root = (project in file(".")).dependsOn(
  db_inject
)

lazy val demo = (project in file("demo")).dependsOn(
  root
).enablePlugins(JettyPlugin).settings(
  mainClass in (Compile, run) := Some("org.sh.easyweb.MyHTMLGen")
)