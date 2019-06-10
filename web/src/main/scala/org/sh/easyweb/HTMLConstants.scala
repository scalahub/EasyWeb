package org.sh.easyweb

import org.sh.reflect.DataStructures.Param
import org.sh.reflect.DataStructures.ScalaMethod
import org.sh.reflect.Util._
import org.sh.reflect.CodeGenUtil._
import org.sh.utils.Util.toDateString

object HTMLConstants {
  // defaults relative urls
  val postUrl:String = "post"
  val fileUploadUrl:String = "upload"
  val fileDownloadUrl = "download"
  val optionTypePrefix = "erjnceijwkxmw2x2oijskqzkoqmzkq"

  def getPage(mainMethods:List[(List[(ScalaMethod, AnyRef)], String)], appInfo:String, callee:AnyRef, pageTitle:String)  = {
    val info = getInfo(mainMethods, appInfo)
    
"<!DOCTYPE html>\n"+"<!--\n"+preamble(callee)+"-->\n"+
s"""
<html>
<head>
<meta content="text/html;charset=utf-8" http-equiv="Content-Type">
<!--<meta content="utf-8" http-equiv="encoding">-->
<title>$pageTitle</title>
"""+CSSConstants.style+"\n"+js(postUrl)+"</head>\n<body onload='changeBGIfLocalhost();'>"+info+classBody(mainMethods)+"\n</body>\n</html>"
  }
  def methodsBody(methods:List[(ScalaMethod, AnyRef)], parent:String) = methods.foldLeft("")((x, y)=> x + "\n"+methodBody(y._1, y._2, parent))
  def classBody(classMethods:List[(List[(ScalaMethod, AnyRef)], String)]) = {
    val sortedMethods = classMethods.map {
      case (list, className) => (list.sortWith{
            case ((method1, _), (method2, _)) => method1.name < method2.name
          }, className
        )
    }
    sortedMethods.foldLeft("")((x, y) => x + "\n<h3 id='"+y._2+"'>"+y._2+" ("+y._1.size+")</h3>"+methodTable(y._1, y._2)+"\n<br>"+methodsBody(y._1, y._2)+"<hr>")
  }
  def methodTable(methods:List[(ScalaMethod, AnyRef)], parent:String) = {
     "<table border=\"1\" style=\"width:100%\" id=\""+parent+
     """.Methods">
      <tr>
        <th>Displayed Name</th>
        <th>Original Name</th>
        <th>Parameters</th>
        <th>Return Type</th>
      </tr>"""+
      methods.map{
        case (sm, c) => "<tr><td><a href=\"#"+parent+"."+sm.name+"\">"+sm.name+
          "</a></td><td>"+sm.origName+"</td><td>"+sm.toScalaParamString+"</td><td>"+sm.cleanReturnedClassName+"</td></tr>"
      }.mkString+"""
    </table>"""
  }
  def getMainClassInfo(mainMethods:List[(List[(ScalaMethod, AnyRef)], String)]) = 
    mainMethods.map{case (fp, c) => (c, fp.size)}

  def getInfo(mainMethods:List[(List[(ScalaMethod, AnyRef)], String)], appInfo:String) = {
    val mainClasses = getMainClassInfo(mainMethods)
    val classesInfo = mainClasses.map{
      case (className, methodCount) => 
        "<a href=\"#"+className+"\">"+
        className+"</a>"+" (<a href=\"#"+className+".Methods\">"+methodCount+"</a>)"
    }
    """  
    <h1 id='top'>Classes ("""+mainClasses.size+""")</h1>
        """+(if (classesInfo.size > 0) classesInfo.reduceLeft(_+"<br>\n"+_) else "")+"""
    <br>
      Total methods found: """+mainMethods.flatMap(_._1).size+"""
    <br><hr>"""+appInfo.replace("\n", "<br>")+"""
    <br>
    <hr>
    """    
  }
  
  def paramInputBox(formID:String, x:Param) = {
    val additionalText = if (x.paraType.getClassName == "java.io.File") {
      """value="" style="background-color:blue; color: #DCDAD1;" READONLY"""
    } else ""   
    
    x.paraType.getClassName match {
      case "byte[]" =>
        s"""    <input id="""".stripMargin+formID+"_"+x.paraName+"""" type="text" name=""""+x.paraName+
        s"""" value="0x"/><br>
           |<label><span></span>Enter bytes as 0x0a1b2c (hex encoded)</label><br>
         """.stripMargin
      case "byte[][]" =>
        s"""    <input id="""".stripMargin+formID+"_"+x.paraName+"""" type="text" name=""""+x.paraName+
        s"""" value="[]"/><br>
           |<label><span></span>Enter array of bytes as [0x01a1,0xffef] (hex encoded)</label><br>
         """.stripMargin
      case "java.lang.String[]" =>
        s"""    <input id=""""+formID+"_"+x.paraName+"""" type="text" name=""""+x.paraName+
        s"""" value="[]"/><br>
           |<label><span></span>Enter strings as [hello,world] (don't use quotes)</label><br>
         """.stripMargin
      case "int[]" =>
        s"""    <input id=""""+formID+"_"+x.paraName+"""" type="text" name=""""+x.paraName+
        s"""" value="[]"/><br>
           |<label><span></span>Enter integers as [1,2,3]</label><br>
         """.stripMargin
      case "boolean[]" =>
        s"""    <input id=""""+formID+"_"+x.paraName+"""" type="text" name=""""+x.paraName+
        s"""" value="[]"/><br>
           |<label><span></span>Enter booleans as [true,false,true]</label><br>
         """.stripMargin
      case any if any.endsWith("]") => // array .. see http://stackoverflow.com/q/3442090/243233
        println("[REFLECT:WARNING] Using default rules for unknown array type: "+any)
        s"""    <input id=""""+formID+"_"+x.paraName+"""" type="text" name=""""+x.paraName+s"""" value="[]"/><br>"""

      case "long" if x.info.map(_ == "date").getOrElse(false) || x.paraName == "from" || x.paraName == "to" || x.paraName == "time" =>
        val paraName = x.paraName
        val inputID = formID+"_"+paraName
        val inputIDInputName = formID+"_"+paraName+"_input"
        val inputDateSelectorID = inputIDInputName+"_selector"
        val inputLongID = inputIDInputName+"_long"
        
        val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
        
        val maxTime = 32472144000000L
        
        s"""   </label>
    <div id="$inputDateSelectorID" > 
      <input type="date" name="$inputID" onchange="dateSelect('$inputID', this.value);" 
       value="${sdf.format(if (x.paraName == "to") maxTime else 0L)}">
      <a href="javascript:void(0);" onclick="toggleHide('$inputDateSelectorID', '$inputLongID');">Epoch</a>
    </div>
    <div id="$inputLongID" style="display:none;"> 
      <input id="$inputID" type="text" name="$inputIDInputName" 
       value="${if (x.paraName == "to") maxTime else 0L}">
      <a href="javascript:void(0);" onclick="toggleHide('$inputDateSelectorID', '$inputLongID');">Date</a>
    </div>
   <label>"""

      case "long"|"int" if x.paraName == "offset" =>
        val value = 0
        s"""    <input id=""""+formID+"_"+x.paraName+"""" type="text" name=""""+x.paraName+s"""" value="$value"/><br>"""        
      case "int" if x.paraName == "max" || x.paraName == "maxRows" =>
        val value = 10
        s"""    <input id=""""+formID+"_"+x.paraName+"""" type="text" name=""""+x.paraName+s"""" value="$value"/><br>"""        
      case "boolean" =>
        val paraName = x.paraName
        val inputID = formID+"_"+paraName
        val inputIDInputName = formID+"_"+paraName+"_input"
        
        val default = x.info match{
          case Some("false") => false
          case _ => true
        }
        def trueChecked = if (default) """ checked="checked" """ else ""
        def falseChecked = if (!default) """ checked="checked" """ else ""
        
        s"""   </label>
    <div>
      <input type="radio" ${trueChecked} name="$inputID" onclick="boolSelect('$inputID', this.value);" value="true">Yes   
      <input type="radio" ${falseChecked} name="$inputID" onclick="boolSelect('$inputID', this.value);" value="false">No
    </div>
    <input id="$inputID" type="hidden" name="$inputIDInputName" value="${default.toString}"><br>
   <label><br/>"""

      case "org.sh.easyweb.Text" =>
        val default = if (x.paraName != "INFO") x.info.getOrElse("").replace("\\n", "\n").replace("\\\\", "\\") else ""
        """    <textarea id=""""+formID+"_"+x.paraName+"""" name=""""+x.paraName+s"""">$default</textarea><br>"""
      case other =>
        val value = if (x.paraName != "INFO") x.info.getOrElse("") else ""
        // $INFO$ is a special variable inside a function,
        // but the "infoVar" of a parameter called "INFO" will also be called $INFO$.
        // Hence we ignore such parameters

        s"""    <input id=""""+formID+"_"+x.paraName+s"""" value="$value" type="text" name=""""+x.paraName+s"""" $additionalText/><br>"""
    }
  }
  def fileInputBox(formID:String, x:Param) = {
    if (x.paraType.getClassName == "java.io.File" || x.paraType.getClassName == "java.nio.file.Path") {
      val inputID = formID+"_"+x.paraName           
      val fileInputID = inputID+"_file"             
      val fileInputIDUpload = fileInputID+"_upload" 
      val inputName = x.paraName+"_file"
      """
    <label><span></span>Please upload a file. Once uploaded, the above box will contain an ID</label>
    <label><span></span>
      <input id=""""+fileInputID+"""" type="file" name=""""+inputName+""""/><br>
    </label>
    <label><span></span>
      <input id=""""+fileInputIDUpload+"""" value="upload" type="button" onclick='JavaScript:uploadFileAndGetID(""""+fileInputID+"""",""""+inputID+"""")'> (<a href="javascript:void(0);" onclick="document.getElementById('"""+inputID+"""').value=''">clear upload</a>)
    </label>
  <hr>
      """
    } else ""      
  }
  def isReturnTypeFile(sm:ScalaMethod) = {
    if (sm.returnType.getClassName == "java.io.File") {}
  }
  def simpleClassName(fqdn:String) = {val i = fqdn.lastIndexOf(".");if (i < 0) fqdn else fqdn.drop(i+1)}
  def methodBody(sm:ScalaMethod, cls:AnyRef, parent:String) = {
    val formID = parent+"."+sm.name
    def parentTag = "<a href=\"#"+parent+"\">parent</a>"
    val parentClass = sm.parentClassName
    val pid = getDefaultPid(cls)
    val ansID = parent+"_"+sm.name+"_Ans"
    val infoStr = if (sm.methodInfo.isDefined)
      """<h4><b>Info</b>: """+sm.methodInfo.get.replace("\\n", "<br>")+"""</h4>"""
    else ""
    
    val resultCSSClass = if (sm.returnType.getClassName == "java.io.File") "file" else "alphaNum"
    
    """
<form id=""""+formID+"""" class="elegant-aero">"""+
    //"\n\n <h1>"+sm.name+"<br><small>["+parent+"."+sm.origName+"]</small></h1>"+infoStr+
    "\n\n <h1><small>"+parent+"."+sm.origName+"</small></h1>"+infoStr+
    sm.params.foldLeft("")((y, x) => y + "\n\n   <label>\n    <span>"+x.paraName+" ["+subst(simpleClassName(x.paraType.getClassName))+"]:</span>\n"+
       paramInputBox(formID, x)+"\n   </label>\n"+fileInputBox(formID, x))+
       """
   <label>
     <span></span>
     <input value="submit" type="button" onclick='JavaScript:xmlhttpPostSend(""""+pid+"""",""""+sm.name+"""",""""+formID+"""","""+jsArray(sm)+""",""""+ansID+"""")'>
   </label>"""+"\n"+ 
       """ 
   <div id=""""+ansID+"""" class=""""+resultCSSClass+"""">"""+"</div>\n"+
  "</form>\n"+topTag+" "+parentTag+"\n"

  }
  def topTag = "<a href=\"#top\">top</a>"
  def jsArray(sm:ScalaMethod) =
    "["+
      (if (sm.params.size == 0) "" else sm.params.map(getParaName).reduceLeft(_+","+_))+
    "]"
  def getParaName(p:Param) = {
    // Modified for handling Option type. Due to type erasure, we only get "Option", and not "Option[String]", etc
    val nameToUse = if (p.paraType.toString == "Lscala/Option;") p.paraName+optionTypePrefix else p.paraName
    "\""+nameToUse+"\""
  }
  def js(postUrl:String) = {
    JSConstants.js
  }
}
