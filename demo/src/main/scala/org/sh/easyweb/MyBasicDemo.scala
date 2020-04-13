package org.sh.easyweb

import org.sh.reflect.{DefaultTypeHandler, EasyProxy}

object MyBasicDemo extends App {
  DefaultTypeHandler.addType[MyType](classOf[MyType], string => new MyType(string), myType => myType.toString)

  val objects = List(
    MyFirstObject,
    MySecondObject,
    new MyFirstClass,
    new MySecondClass,
    MiscErrors,
    Foo
  )
  List("*Restricted*").foreach(EasyProxy.preventMethod)
  new AutoWeb(objects, "Sample app")
}
