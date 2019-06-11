package org.sh.easyweb

import java.io.File

import org.sh.utils.json.JSONUtil.JsonFormatted

class MyType(val string:String) {
  override def toString = s"MyType($string)"
}

case class Foo(s:String, a:Int, b:BigInt) {
  override def toString = s"($s -> $a -> $b)"
}

case class Bar(a:String, b:Int, c:BigInt, d:Array[String], e:Array[Int]) extends JsonFormatted {
  val keys = Array("a", "b", "c", "d", "e")
  val vals = Array(a, b, c, d, e)
}

class MyFirstClass {

  def myMethod_Foos(i:Int) = {
    (0 to i).toArray.map{j =>
      val s = j.toString
      Foo(s, j, j)
    }
  }

  def myMethod_Bars(i:Int) = {
    (0 to i).toArray.map{j =>
      val s = j.toString
      Bar(s, j, j, Array(s, s, s), Array(1, 2, 3, 4))
    }
  }

  def myMethod_Foo = {
    Foo("s", 10, 10)
  }

  def myMethod_Bar = {
    Bar("s", 10, 10, Array("s", "s", "s"), Array(1, 2, 3, 4))
  }

  def myMethod_InfoVars(choice:Boolean, value:Int) = {
    val $INFO$ = "Info about someMethod. Check that choice is 'false' and value is '29'"
    val $choice$ = "false" // should set default choice radio button to "no"
    val $value$ = "29" // should set default value to "29"
    value.toString
  }
  def myMethod_Arrays(a:Array[Int], b:Array[String], c:Array[Boolean], d:Array[Byte], e:Array[Array[Byte]]) = {
    val s = a.sum + b.map(_.length).sum
    val r = c.foldLeft(true)((x, y) => x && y)
    val t = if (r) s else -s
    Array(t, 1, 2, 3)
  }
  def myMethod_Option(a:Option[String], b:Option[String]) = {
    Array(
      "Parameter a is: "+a,
      "Parameter b is: "+b
    )
  }

  def otherMethod(choice:Boolean, arrFiles:Array[File]) = {
    // should not appear
    "Ok"
  }
}

class MySecondClass extends MyFirstClass {
  def myMethod_FileUpload(file:File) = {
    val $INFO$ = "File upload test. Should print the size of file"
    val $file$ = "This box will contain a file id after upload"
    file.length()
  }
  def myMethod_MyType(myType:MyType) = {
    new MyType(myType.string.reverse)
  }
}

object MyFirstObject {
  def myMethod_Choice(choice:Boolean) = {
    "Ok"
  }
  def myMethod_FileDownload(file:File) = {
    val $INFO$ = "Should return the same file back"
    file
  }
}

object MySecondObject extends MySecondClass {
  def myMethod_TextBox(text:Text) = {
    val $INFO$ = "Accepts text box"
    s"Text of ${text.getText.size} chars"
  }
  def myMethod_Restricted(text:Text) = {
    val $INFO$ = "This method is restricted. Try calling it."
    s"Text of ${text.getText.size} chars"
  }
}

