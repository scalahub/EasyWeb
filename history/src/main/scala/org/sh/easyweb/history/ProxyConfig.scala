
package org.sh.easyweb.history

import org.sh.utils.common.file.PlaintextFileProperties
import org.sh.utils.common.file.TraitPlaintextFileProperties
import org.sh.easyweb.HTMLClientCodeGenerator
import org.sh.reflect.DefaultTypeHandler
import org.sh.reflect.Proxy

trait WebProxyConfig extends TraitPlaintextFileProperties {
  lazy val propertyFile = "misc.properties"
  
  val restrict:Array[String] = read("restrictMethods", "").split(";").filterNot(_.isEmpty) // Array("*.fiat.*")
  val includeOnlyFile = read("includeOnlyFile", "webAutoGen.properties")
  val includeOnlyProps = new PlaintextFileProperties(includeOnlyFile)

  val formInfo:String
  val formUrl:String
  val htmlFileName:String
  val htmlFileDir:String
  val pageTitle:String
  val pageTitleFiltered:String

  val prefixString:String = "" // only methods beginning with prefix will be loaded/generated. If prefix is empty, all methods will be used

  def toDo = read("toDo", "").split(';')
  
  val formObjects:Set[Object]
  lazy val cg = new HTMLClientCodeGenerator(formObjects.toList.sortWith{(l, r) => l.getClass.getCanonicalName < r.getClass.getCanonicalName}, formUrl, formInfo, None, false, false)

  val formClasses:List[AnyRef]
  
  val autoStart:List[Object]
}
class InitializeProxyConfig(config:WebProxyConfig){
  import config._
  formObjects.foreach(Proxy.addProcessor(prefixString, _, DefaultTypeHandler, true))
  formClasses.foreach(Proxy.addProcessor(prefixString, _, DefaultTypeHandler, true))
  restrict.foreach(Proxy.preventMethod)
  autoStart.foreach{c =>
    println(s" [${this.getClass.getName}] started ${c.getClass.getCanonicalName}")  
  }
}
class HTMLGenerator(config:WebProxyConfig) {  
  import config._
  def main(args:Array[String]) { 
    println("Auto-Generating file")
    formClasses.foreach{o => cg.c :+= o}
    val list = try includeOnlyProps.props.getProperty("includeOnly").split(";", -1).filter(_ != "").map(_.split("#", -1)).map(x => (x(1), x(0))).toList 
               catch {case e:Throwable => Nil}
    try {
      cg.pageTitle = pageTitle
      cg.autoGenerateToFile(htmlFileName, htmlFileDir, prefixString)    	
      if (list.size > 0 ) {
        cg.pageTitle = pageTitleFiltered
        cg.autoGenerateToFileFiltered(htmlFileName+"filter", htmlFileDir, list, prefixString)    		
      }
    } catch { case t:Throwable => t.printStackTrace } 
    finally System.exit(1)
  }
}
