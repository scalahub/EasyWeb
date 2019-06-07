package org.sh.easyweb

import MyConfig._
import org.sh.reflect.DefaultTypeHandler

// similar to MyAdvancedDemo but uses AutoWeb to auto-wire all the components
object MyBasicDemo extends App {
  DefaultTypeHandler.addType[MyType](classOf[MyType], string => new MyType(string), myType => myType.toString)

  val objects = List(
    MyFirstObject,
    MySecondObject,
    myFirstClass,
    mySecondClass
  )
  new AutoWeb(objects, "Sample app")
}
