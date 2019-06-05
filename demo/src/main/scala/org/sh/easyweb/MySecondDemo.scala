package org.sh.easyweb

import MyConfig._

// similar to MyDemo but uses AutoWeb
object MySecondDemo extends App {
  val objects = List(
    MyFirstObject,
    MySecondObject,
    myFirstClass,
    mySecondClass
  )
  new AutoWeb(objects, "Sample app")
}
