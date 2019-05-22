name := "EasyWeb"

version := "0.1"

scalaVersion := "2.12.8"

lazy val EasyMirror = RootProject(uri("https://github.com/scalahub/EasyMirror.git"))
//lazy val EasyMirror = RootProject(uri("../EasyMirror"))

lazy val BetterDB = RootProject(uri("https://github.com/scalahub/BetterDB.git"))
//lazy val BetterDB = RootProject(uri("../BetterDB"))

lazy val web = (project in file("web")).dependsOn(EasyMirror)

lazy val history = (project in file("history")).dependsOn(web, BetterDB)

lazy val root = project in file(".") dependsOn (history)
