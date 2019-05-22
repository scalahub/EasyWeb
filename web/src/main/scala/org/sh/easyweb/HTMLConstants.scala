package org.sh.easyweb

//import org.sh.reflect.DataStructures.Param
import org.sh.reflect.DataStructures.Param
import org.sh.reflect.DataStructures.ScalaMethod
import org.sh.reflect.Util._
import org.sh.reflect.CodeGenUtil._
import org.sh.utils.common.Util.toDateString

object HTMLConstants {
//  def clean(s:String) = s.replace("$", "")
  def getPage(mainMethods:List[(List[(ScalaMethod, AnyRef)], String)], appInfo:String, postUrl:String, callee:AnyRef, pageTitle:String)  = { 
    val info = getInfo(mainMethods, appInfo)
    
"<!DOCTYPE html>\n"+"<!--\n"+preamble(callee)+"-->\n"+
s"""
<html>
<head>
<meta content="text/html;charset=utf-8" http-equiv="Content-Type">
<!--<meta content="utf-8" http-equiv="encoding">-->
<title>$pageTitle</title>
"""+style+"\n"+js(postUrl)+"</head>\n<body onload='changeBGIfLocalhost();'>"+info+classBody(mainMethods)+"\n</body>\n</html>"
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
//        case (sm, c) => "<tr><td><a href=\"#"+clean(c.getClass.getCanonicalName)+"."+sm.name+"\">"+sm.name+
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
//      println("====================================> FILE")
      """value="" style="background-color:blue; color: #DCDAD1;" READONLY"""
    } else ""   
    
    x.paraType.getClassName match {
      case any if any.endsWith("]") => // array .. see http://stackoverflow.com/q/3442090/243233
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
//        s"""   </label>
//    <div><input type="radio" name="$inputID" onclick="boolSelect('$inputID', this.value);" value="true">Yes</div>   
//    <div><input type="radio" name="$inputID" onclick="boolSelect('$inputID', this.value);" value="false">No</div>
//    <input id="$inputID" type="hidden" name="$inputIDInputName" value="">
//   <label>"""
        
      case "org.sh.easyweb.Text" =>
        val default = if (x.paraName != "INFO") x.info.getOrElse("").replace("\\n", "\n").replace("\\\\", "\\") else ""
        """    <textarea id=""""+formID+"_"+x.paraName+"""" name=""""+x.paraName+s"""">$default</textarea><br>"""
//      case "scala.Option" =>
//        println(" ||||| ===> "+x.paraType.get)
//        println(" ||||| ===> "+x.paraType.getInternalName)
//        println(" ||||| ===> "+x.paraType.getElementType.getDescriptor)
//        println(" ||||| ===> "+x.paraType.getArgumentTypes)
//        try x.paraType.getArgumentTypes.foreach(a => try println(" ||| => "+a) finally {}) finally {}
//        println(" ||||| ===> "+x.paraType.getArgumentsAndReturnSizes)
//        s"""    <input id=""""+formID+"_"+x.paraName+"""" type="text" name=""""+x.paraName+s"""" $additionalText/><br>"""                
      case other =>
        val value = if (x.paraName != "INFO") x.info.getOrElse("") else ""
        // $INFO$ is a special variable inside a function,
        // but the "infoVar" of a parameter called "INFO" will also be called $INFO$.
        // Hence we ignore such parameters
        // 
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
  def jsArray(sm:ScalaMethod) = "["+
      (if (sm.params.size == 0) "" else sm.params.map("\""+_.paraName+"\"").reduceLeft(_+","+_))+"]"
    
  val style = 
  """
<style type="text/css">
/* Elegant Aero */
.elegant-aero {
    margin-left:auto;
    margin-right:auto;

    max-width: 1280px;
    /* max-width: 700px; */
    background: #D2E9FF;
    padding: 20px 20px 20px 20px;
    font: 12px Arial, Helvetica, sans-serif;
    color: #666;
}
.elegant-aero h1 {
    font: 24px "Trebuchet MS", Arial, Helvetica, sans-serif;
    padding: 10px 10px 10px 20px;
    display: block;
    background: #C0E1FF;
    border-bottom: 1px solid #B8DDFF;
    margin: -20px -20px 15px;
}
.elegant-aero h1>span {
    display: block;
    font-size: 11px;
}

.elegant-aero label>span {
    float: left;
    margin-top: 10px;
    color: #5E5E5E;
}
.elegant-aero label {
    display: block;
    margin: 0px 0px 5px;
}
.elegant-aero label>span {
    float: left;
    width: 20%;
    text-align: right;
    padding-right: 15px;
    margin-top: 10px;
    font-weight: bold;
}
.elegant-aero input[type="text"], .elegant-aero input[type="email"], .elegant-aero textarea, .elegant-aero select {
    color: #888;
    width: 70%;
    padding: 0px 0px 0px 5px;
    border: 1px solid #C5E2FF;
    background: #FBFBFB;
    outline: 0;
    -webkit-box-shadow:inset 0px 1px 6px #ECF3F5;
    box-shadow: inset 0px 1px 6px #ECF3F5;
    font: 200 12px/25px Arial, Helvetica, sans-serif;
    height: 30px;
    line-height:15px;
    margin: 2px 6px 16px 0px;
}
.elegant-aero textarea{
    height:100px;
    padding: 5px 0px 0px 5px;
    width: 70%;
}
.elegant-aero select {
    background: #fbfbfb url('down-arrow.png') no-repeat right;
    background: #fbfbfb url('down-arrow.png') no-repeat right;
   appearance:none;
    -webkit-appearance:none;
   -moz-appearance: none;
    text-indent: 0.01px;
    text-overflow: '';
    width: 70%;
}
.elegant-aero .button{
    padding: 10px 30px 10px 30px;
    background: #66C1E4;
    border: none;
    color: #FFF;
    box-shadow: 1px 1px 1px #4C6E91;
    -webkit-box-shadow: 1px 1px 1px #4C6E91;
    -moz-box-shadow: 1px 1px 1px #4C6E91;
    text-shadow: 1px 1px 1px #5079A3;
   
}
.elegant-aero .button:hover{
    background: #3EB1DD;
}
p {
    font-family: Lucida Sans Typewriter,Lucida Console,monaco,Bitstream Vera Sans Mono,monospace;
    font-size: 14px;
    font-style: normal;
    font-variant: normal;
    font-weight: 400;
    line-height: 20px;
}  
</style>
  """
  def js(postUrl:String) = 
  """
<script type="text/javascript">
/**
 * JavaScript format string function
 * 
 */
String.prototype.format = function()
{
  var args = arguments;

  return this.replace(/{(\d+)}/g, function(match, number)
  {
    return typeof args[number] != 'undefined' ? args[number] :
                                                '{' + number + '}';
  });
};


/**
 * Convert a Javascript Oject array or String array to an HTML table
 * JSON parsing has to be made before function call
 * It allows use of other JSON parsing methods like jQuery.parseJSON
 * http(s)://, ftp://, file:// and javascript:; links are automatically computed
 *
 * JSON data samples that should be parsed and then can be converted to an HTML table
 *     var objectArray = '[{"Total":"34","Version":"1.0.4","Office":"New York"},{"Total":"67","Version":"1.1.0","Office":"Paris"}]';
 *     var stringArray = '["New York","Berlin","Paris","Marrakech","Moscow"]';
 *     var nestedTable = '[{ key1: "val1", key2: "val2", key3: { tableId: "tblIdNested1", tableClassName: "clsNested", linkText: "Download", data: [{ subkey1: "subval1", subkey2: "subval2", subkey3: "subval3" }] } }]'; 
 *
 * Code sample to create a HTML table Javascript String
 *     var jsonHtmlTable = ConvertJsonToTable(eval(dataString), 'jsonTable', null, 'Download');
 *
 * Code sample explaned
 *  - eval is used to parse a JSON dataString
 *  - table HTML id attribute will be 'jsonTable'
 *  - table HTML class attribute will not be added
 *  - 'Download' text will be displayed instead of the link itself
 *
 * @author Afshin Mehrabani <afshin dot meh at gmail dot com>
 * 
 * @class ConvertJsonToTable
 * 
 * @method ConvertJsonToTable
 * 
 * @param parsedJson object Parsed JSON data
 * @param tableId string Optional table id 
 * @param tableClassName string Optional table css class name
 * @param linkText string Optional text replacement for link pattern
 *  
 * @return string Converted JSON to HTML table
 */
function isArray(what) {
    return Object.prototype.toString.call(what) === '[object Array]';
}
function ConvertJsonToTable(parsedJson, tableId, tableClassName, linkText)
{
    //Patterns for links and NULL value
    var italic = '<i>{0}</i>';
    var link = linkText ? '<a href="{0}">' + linkText + '</a>' :
                          '<a href="{0}">{0}</a>';

    //Pattern for table                          
    var idMarkup = tableId ? ' id="' + tableId + '"' :
                             '';

    var classMarkup = tableClassName ? ' class="' + tableClassName + '"' :
                                       '';

    var tbl = '<table border="1" cellpadding="0" cellspacing="0"' + idMarkup + classMarkup + '>{0}{1}</table>';

    //Patterns for table content
    var th = '<thead>{0}</thead>';
    var tb = '<tbody>{0}</tbody>';
    var tr = '<tr>{0}</tr>';
    var thRow = '<th></th><th align="left">&nbsp;{0}&nbsp;</th>';
    var tdRow = '<td>{1}</td><td>{0}</td>';
    var thCon = '';
    var tbCon = '';
    var trCon = '';

	var headers;
	
	if(isArray(parsedJson))
	{
		var rows = parsedJson.length;
		thCon += thRow.format('Rows: '+rows);
		th = th.format(tr.format(thCon));
		for (i = 0; i < parsedJson.length; i++)
		{
			tbCon += tdRow.format(parsedJson[i], i+1);
			trCon += tr.format(tbCon);
			tbCon = '';
		}
	}

	else
	{
		thCon += thRow.format(JSON.stringify(parsedJson));
		// thCon += thRow.format(parsedJson);
		th = th.format(tr.format(thCon));
	}
	tb = tb.format(trCon);
	tbl = tbl.format(th, tb);

	return tbl;
}


/**
 * Return just the keys from the input array, optionally only for the specified search_value
 * version: 1109.2015
 *  discuss at: http://phpjs.org/functions/array_keys
 *  +   original by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
 *  +      input by: Brett Zamir (http://brett-zamir.me)
 *  +   bugfixed by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
 *  +   improved by: jd
 *  +   improved by: Brett Zamir (http://brett-zamir.me)
 *  +   input by: P
 *  +   bugfixed by: Brett Zamir (http://brett-zamir.me)
 *  *     example 1: array_keys( {firstname: 'Kevin', surname: 'van Zonneveld'} );
 *  *     returns 1: {0: 'firstname', 1: 'surname'}
 */
function array_keys(input, search_value, argStrict)
{
    var search = typeof search_value !== 'undefined', tmp_arr = [], strict = !!argStrict, include = true, key = '';

    if (input && typeof input === 'object' && input.change_key_case) { // Duck-type check for our own array()-created PHPJS_Array
        return input.keys(search_value, argStrict);
    }
 
    for (key in input)
    {
        if (input.hasOwnProperty(key))
        {
            include = true;
            if (search)
            {
                if (strict && input[key] !== search_value)
                    include = false;
                else if (input[key] != search_value)
                    include = false;
            } 
            if (include)
                tmp_arr[tmp_arr.length] = key;
        }
    }
    return tmp_arr;
}
var Base64={_keyStr:"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",encode:function(e){var t="";var n,r,i,s,o,u,a;var f=0;e=Base64._utf8_encode(e);while(f<e.length){n=e.charCodeAt(f++);r=e.charCodeAt(f++);i=e.charCodeAt(f++);s=n>>2;o=(n&3)<<4|r>>4;u=(r&15)<<2|i>>6;a=i&63;if(isNaN(r)){u=a=64}else if(isNaN(i)){a=64}t=t+this._keyStr.charAt(s)+this._keyStr.charAt(o)+this._keyStr.charAt(u)+this._keyStr.charAt(a)}return t},decode:function(e){var t="";var n,r,i;var s,o,u,a;var f=0;e=e.replace(/[^A-Za-z0-9\+\/\=]/g,"");while(f<e.length){s=this._keyStr.indexOf(e.charAt(f++));o=this._keyStr.indexOf(e.charAt(f++));u=this._keyStr.indexOf(e.charAt(f++));a=this._keyStr.indexOf(e.charAt(f++));n=s<<2|o>>4;r=(o&15)<<4|u>>2;i=(u&3)<<6|a;t=t+String.fromCharCode(n);if(u!=64){t=t+String.fromCharCode(r)}if(a!=64){t=t+String.fromCharCode(i)}}t=Base64._utf8_decode(t);return t},_utf8_encode:function(e){e=e.replace(/\r\n/g,"\n");var t="";for(var n=0;n<e.length;n++){var r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r)}else if(r>127&&r<2048){t+=String.fromCharCode(r>>6|192);t+=String.fromCharCode(r&63|128)}else{t+=String.fromCharCode(r>>12|224);t+=String.fromCharCode(r>>6&63|128);t+=String.fromCharCode(r&63|128)}}return t},_utf8_decode:function(e){var t="";var n=0;var r=c1=c2=0;while(n<e.length){r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r);n++}else if(r>191&&r<224){c2=e.charCodeAt(n+1);t+=String.fromCharCode((r&31)<<6|c2&63);n+=2}else{c2=e.charCodeAt(n+1);c3=e.charCodeAt(n+2);t+=String.fromCharCode((r&15)<<12|(c2&63)<<6|c3&63);n+=3}}return t}}
function xmlhttpPostSend(pid, reqName, formid, params, ansid) {
  	var status = '';
	try {
          //var reqID = Math.random().toString();
          var qryStr = getquerystringSend(ansid, pid, reqName, formid, params)
          //var qryStr = getquerystringSend(reqID, pid, reqName, formid, params)
          var xmlHttpReq = false;
          var self = this;
          // Mozilla/Safari
          if (window.XMLHttpRequest) {
                  self.xmlHttpReq = new XMLHttpRequest();
          }
          // IE
          else if (window.ActiveXObject) {
                  self.xmlHttpReq = new ActiveXObject("Microsoft.XMLHTTP");
          }
          var startTime = 0;
          self.xmlHttpReq.open('POST', "/web", true);
          self.xmlHttpReq.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
          self.xmlHttpReq.onreadystatechange = function() {
                  if (self.xmlHttpReq.readyState == 4) {
                          updateSend(self.xmlHttpReq.responseText, start); 
                  }
          }
		  
          function errorHandler(event){
                document.getElementById(ansid).innerHTML = "Request Failed";
          }
          function abortHandler(event){
                document.getElementById(ansid).innerHTML = "Request Aborted";
          }
          self.xmlHttpReq.addEventListener("error", errorHandler, false);
          self.xmlHttpReq.addEventListener("abort", abortHandler, false);
          start = window.performance.now();
          self.xmlHttpReq.send(qryStr);
          // status = 'making request: '+qryStr
          status = 'making request: '+reqName;
	}  
	catch(err) {
          status = err; 
	}  
	document.getElementById(ansid).innerHTML = status;
}
function uploadFileAndGetID(fileid, uploadresultid) {
	var formData = new FormData(),
		file = document.getElementById(fileid).files[0];
	var xmlHttpReq = false;
	var self = this;
	// Mozilla/Safari
	if (window.XMLHttpRequest) {
		  self.xmlHttpReq = new XMLHttpRequest();
	}
	// IE
	else if (window.ActiveXObject) {
		  self.xmlHttpReq = new ActiveXObject("Microsoft.XMLHTTP");
	}
	function progressHandler(event){
		var percent = (event.loaded / event.total) * 100;
		var msg = Math.round(percent)+"% uploaded... please wait";
		document.getElementById(uploadresultid).value = msg;
	}
	self.xmlHttpReq.open('POST', "/putfile", true);
	formData.append('fileUpload', file);
	self.xmlHttpReq.onreadystatechange = function() {
		if (self.xmlHttpReq.readyState == 4) {
			 document.getElementById(uploadresultid).value = self.xmlHttpReq.responseText.trim();
		}
	}
	function errorHandler(event){
		document.getElementById(uploadresultid).value = "Upload Failed";
	}
	function abortHandler(event){
		document.getElementById(uploadresultid).value = "Upload Aborted";
	}
	self.xmlHttpReq.upload.addEventListener("progress", progressHandler, false);
	self.xmlHttpReq.addEventListener("error", errorHandler, false);
	self.xmlHttpReq.addEventListener("abort", abortHandler, false);
	self.xmlHttpReq.send(formData);
	document.getElementById(uploadresultid).value = "please wait while the file is uploaded ...";  
	
}

function encodejson(formid, paraNames) {
	var arrayLength = paraNames.length;
	var json = { };
	for (var i = 0; i < arrayLength; i++) {
		var itemid = formid + "_" + paraNames[i];               
		var value = document.getElementById(itemid).value;
		if (value == '') throw "Error: parameter "+paraNames[i]+" cannot be empty";
		var key = paraNames[i];
                if (document.getElementById(itemid).type == 'textarea') {
                    value = Base64.encode(value);
                } else {
                    value = value.trim();
                }
		json[key] = value;
	}
	var jsonstring = JSON.stringify(json);
	return jsonstring;
}
function getquerystringSend(reqID, pid, reqName, formid, params) {
	var reqdata = Base64.encode(encodejson(formid, params));
	qstr = 'reqId='+escape(reqID)+'&pid='+escape(pid)+'&reqName='+escape(reqName)+'&reqData='+escape(reqdata);
	// console.log(" => "+qstr)
	return qstr;
}
  
/*
    Copyright 2008-2013
        Matthias Ehmann,
        Michael Gerhaeuser,
        Carsten Miller,
        Bianca Valentin,
        Alfred Wassermann,
        Peter Wilfahrt


    Dual licensed under the Apache License Version 2.0, or LGPL Version 3 licenses.

    You should have received a copy of the GNU Lesser General Public License
    along with JSXCompressor.  If not, see <http://www.gnu.org/licenses/>.

    You should have received a copy of the Apache License along with JSXCompressor.
    If not, see <http://www.apache.org/licenses/>.
*/
(function(){var e,r,n;(function(t){function o(e,r){return C.call(e,r)}function i(e,r){var n,t,o,i,a,u,c,f,s,l,p=r&&r.split("/"),h=k.map,d=h&&h["*"]||{};if(e&&"."===e.charAt(0))if(r){for(p=p.slice(0,p.length-1),e=p.concat(e.split("/")),f=0;e.length>f;f+=1)if(l=e[f],"."===l)e.splice(f,1),f-=1;else if(".."===l){if(1===f&&(".."===e[2]||".."===e[0]))break;f>0&&(e.splice(f-1,2),f-=2)}e=e.join("/")}else 0===e.indexOf("./")&&(e=e.substring(2));if((p||d)&&h){for(n=e.split("/"),f=n.length;f>0;f-=1){if(t=n.slice(0,f).join("/"),p)for(s=p.length;s>0;s-=1)if(o=h[p.slice(0,s).join("/")],o&&(o=o[t])){i=o,a=f;break}if(i)break;!u&&d&&d[t]&&(u=d[t],c=f)}!i&&u&&(i=u,a=c),i&&(n.splice(0,a,i),e=n.join("/"))}return e}function a(e,r){return function(){return h.apply(t,v.call(arguments,0).concat([e,r]))}}function u(e){return function(r){return i(r,e)}}function c(e){return function(r){b[e]=r}}function f(e){if(o(m,e)){var r=m[e];delete m[e],y[e]=!0,p.apply(t,r)}if(!o(b,e)&&!o(y,e))throw Error("No "+e);return b[e]}function s(e){var r,n=e?e.indexOf("!"):-1;return n>-1&&(r=e.substring(0,n),e=e.substring(n+1,e.length)),[r,e]}function l(e){return function(){return k&&k.config&&k.config[e]||{}}}var p,h,d,g,b={},m={},k={},y={},C=Object.prototype.hasOwnProperty,v=[].slice;d=function(e,r){var n,t=s(e),o=t[0];return e=t[1],o&&(o=i(o,r),n=f(o)),o?e=n&&n.normalize?n.normalize(e,u(r)):i(e,r):(e=i(e,r),t=s(e),o=t[0],e=t[1],o&&(n=f(o))),{f:o?o+"!"+e:e,n:e,pr:o,p:n}},g={require:function(e){return a(e)},exports:function(e){var r=b[e];return r!==void 0?r:b[e]={}},module:function(e){return{id:e,uri:"",exports:b[e],config:l(e)}}},p=function(e,r,n,i){var u,s,l,p,h,k,C=[];if(i=i||e,"function"==typeof n){for(r=!r.length&&n.length?["require","exports","module"]:r,h=0;r.length>h;h+=1)if(p=d(r[h],i),s=p.f,"require"===s)C[h]=g.require(e);else if("exports"===s)C[h]=g.exports(e),k=!0;else if("module"===s)u=C[h]=g.module(e);else if(o(b,s)||o(m,s)||o(y,s))C[h]=f(s);else{if(!p.p)throw Error(e+" missing "+s);p.p.load(p.n,a(i,!0),c(s),{}),C[h]=b[s]}l=n.apply(b[e],C),e&&(u&&u.exports!==t&&u.exports!==b[e]?b[e]=u.exports:l===t&&k||(b[e]=l))}else e&&(b[e]=n)},e=r=h=function(e,r,n,o,i){return"string"==typeof e?g[e]?g[e](r):f(d(e,r).f):(e.splice||(k=e,r.splice?(e=r,r=n,n=null):e=t),r=r||function(){},"function"==typeof n&&(n=o,o=i),o?p(t,e,r,n):setTimeout(function(){p(t,e,r,n)},4),h)},h.config=function(e){return k=e,k.deps&&h(k.deps,k.callback),h},n=function(e,r,n){r.splice||(n=r,r=[]),o(b,e)||o(m,e)||(m[e]=[e,r,n])},n.amd={jQuery:!0}})(),n("../node_modules/almond/almond",function(){}),n("jxg",[],function(){var e={};return"object"!=typeof JXG||JXG.extend||(e=JXG),e.extend=function(e,r,n,t){var o,i;n=n||!1,t=t||!1;for(o in r)(!n||n&&r.hasOwnProperty(o))&&(i=t?o.toLowerCase():o,e[i]=r[o])},e.extend(e,{boards:{},readers:{},elements:{},registerElement:function(e,r){e=e.toLowerCase(),this.elements[e]=r},registerReader:function(e,r){var n,t;for(n=0;r.length>n;n++)t=r[n].toLowerCase(),"function"!=typeof this.readers[t]&&(this.readers[t]=e)},shortcut:function(e,r){return function(){return e[r].apply(this,arguments)}},getRef:function(e,r){return e.select(r)},getReference:function(e,r){return e.select(r)},debugInt:function(){var e,r;for(e=0;arguments.length>e;e++)r=arguments[e],"object"==typeof window&&window.console&&console.log?console.log(r):"object"==typeof document&&document.getElementById("debug")&&(document.getElementById("debug").innerHTML+=r+"<br/>")},debugWST:function(){var r=Error();e.debugInt.apply(this,arguments),r&&r.stack&&(e.debugInt("stacktrace"),e.debugInt(r.stack.split("\n").slice(1).join("\n")))},debugLine:function(){var r=Error();e.debugInt.apply(this,arguments),r&&r.stack&&e.debugInt("Called from",r.stack.split("\n").slice(2,3).join("\n"))},debug:function(){e.debugInt.apply(this,arguments)}}),e}),n("utils/zip",["jxg"],function(e){var r=[0,128,64,192,32,160,96,224,16,144,80,208,48,176,112,240,8,136,72,200,40,168,104,232,24,152,88,216,56,184,120,248,4,132,68,196,36,164,100,228,20,148,84,212,52,180,116,244,12,140,76,204,44,172,108,236,28,156,92,220,60,188,124,252,2,130,66,194,34,162,98,226,18,146,82,210,50,178,114,242,10,138,74,202,42,170,106,234,26,154,90,218,58,186,122,250,6,134,70,198,38,166,102,230,22,150,86,214,54,182,118,246,14,142,78,206,46,174,110,238,30,158,94,222,62,190,126,254,1,129,65,193,33,161,97,225,17,145,81,209,49,177,113,241,9,137,73,201,41,169,105,233,25,153,89,217,57,185,121,249,5,133,69,197,37,165,101,229,21,149,85,213,53,181,117,245,13,141,77,205,45,173,109,237,29,157,93,221,61,189,125,253,3,131,67,195,35,163,99,227,19,147,83,211,51,179,115,243,11,139,75,203,43,171,107,235,27,155,91,219,59,187,123,251,7,135,71,199,39,167,103,231,23,151,87,215,55,183,119,247,15,143,79,207,47,175,111,239,31,159,95,223,63,191,127,255],n=[3,4,5,6,7,8,9,10,11,13,15,17,19,23,27,31,35,43,51,59,67,83,99,115,131,163,195,227,258,0,0],t=[0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5,0,99,99],o=[1,2,3,4,5,7,9,13,17,25,33,49,65,97,129,193,257,385,513,769,1025,1537,2049,3073,4097,6145,8193,12289,16385,24577],i=[0,0,0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13],a=[16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15],u=256;return e.Util=e.Util||{},e.Util.Unzip=function(c){function f(){return R+=8,O>X?c[X++]:-1}function s(){B=1}function l(){var e;try{return R++,e=1&B,B>>=1,0===B&&(B=f(),e=1&B,B=128|B>>1),e}catch(r){throw r}}function p(e){var n=0,t=e;try{for(;t--;)n=n<<1|l();e&&(n=r[n]>>8-e)}catch(o){throw o}return n}function h(){J=0}function d(e){j++,G[J++]=e,z.push(String.fromCharCode(e)),32768===J&&(J=0)}function g(){this.b0=0,this.b1=0,this.jump=null,this.jumppos=-1}function b(){for(;;){if(M[H]>=x)return-1;if(U[M[H]]===H)return M[H]++;M[H]++}}function m(){var e,r=P[F];if(17===H)return-1;if(F++,H++,e=b(),e>=0)r.b0=e;else if(r.b0=32768,m())return-1;if(e=b(),e>=0)r.b1=e,r.jump=null;else if(r.b1=32768,r.jump=P[F],r.jumppos=F,m())return-1;return H--,0}function k(e,r,n){var t;for(P=e,F=0,U=n,x=r,t=0;17>t;t++)M[t]=0;return H=0,m()?-1:0}function y(e){for(var r,n,t,o=0,i=e[o];;)if(t=l()){if(!(32768&i.b1))return i.b1;for(i=i.jump,r=e.length,n=0;r>n;n++)if(e[n]===i){o=n;break}}else{if(!(32768&i.b0))return i.b0;o++,i=e[o]}}function C(){var u,c,b,m,C,v,A,j,w,U,x,S,z,I,E,L,O;do if(u=l(),b=p(2),0===b)for(s(),U=f(),U|=f()<<8,S=f(),S|=f()<<8,65535&(U^~S)&&e.debug("BlockLen checksum mismatch\n");U--;)c=f(),d(c);else if(1===b)for(;;)if(C=r[p(7)]>>1,C>23?(C=C<<1|l(),C>199?(C-=128,C=C<<1|l()):(C-=48,C>143&&(C+=136))):C+=256,256>C)d(C);else{if(256===C)break;for(C-=257,w=p(t[C])+n[C],C=r[p(5)]>>3,i[C]>8?(x=p(8),x|=p(i[C]-8)<<8):x=p(i[C]),x+=o[C],C=0;w>C;C++)c=G[32767&J-x],d(c)}else if(2===b){for(A=Array(320),I=257+p(5),E=1+p(5),L=4+p(4),C=0;19>C;C++)A[C]=0;for(C=0;L>C;C++)A[a[C]]=p(3);for(w=q.length,m=0;w>m;m++)q[m]=new g;if(k(q,19,A,0))return h(),1;for(z=I+E,m=0,O=-1;z>m;)if(O++,C=y(q),16>C)A[m++]=C;else if(16===C){if(C=3+p(2),m+C>z)return h(),1;for(v=m?A[m-1]:0;C--;)A[m++]=v}else{if(C=17===C?3+p(3):11+p(7),m+C>z)return h(),1;for(;C--;)A[m++]=0}for(w=T.length,m=0;w>m;m++)T[m]=new g;if(k(T,I,A,0))return h(),1;for(w=T.length,m=0;w>m;m++)q[m]=new g;for(j=[],m=I;A.length>m;m++)j[m-I]=A[m];if(k(q,E,j,0))return h(),1;for(;;)if(C=y(T),C>=256){if(C-=256,0===C)break;for(C-=1,w=p(t[C])+n[C],C=y(q),i[C]>8?(x=p(8),x|=p(i[C]-8)<<8):x=p(i[C]),x+=o[C];w--;)c=G[32767&J-x],d(c)}else d(C)}while(!u);return h(),s(),0}function v(){var e,r,n,t,o,i,a,c,s=[];try{if(z=[],L=!1,s[0]=f(),s[1]=f(),120===s[0]&&218===s[1]&&(C(),E[I]=[z.join(""),"geonext.gxt"],I++),31===s[0]&&139===s[1]&&(S(),E[I]=[z.join(""),"file"],I++),80===s[0]&&75===s[1]&&(L=!0,s[2]=f(),s[3]=f(),3===s[2]&&4===s[3])){for(s[0]=f(),s[1]=f(),A=f(),A|=f()<<8,c=f(),c|=f()<<8,f(),f(),f(),f(),a=f(),a|=f()<<8,a|=f()<<16,a|=f()<<24,i=f(),i|=f()<<8,i|=f()<<16,i|=f()<<24,o=f(),o|=f()<<8,o|=f()<<16,o|=f()<<24,t=f(),t|=f()<<8,n=f(),n|=f()<<8,e=0,N=[];t--;)r=f(),"/"===r|":"===r?e=0:u-1>e&&(N[e++]=String.fromCharCode(r));for(w||(w=N),e=0;n>e;)r=f(),e++;j=0,8===c&&(C(),E[I]=Array(2),E[I][0]=z.join(""),E[I][1]=N.join(""),I++),S()}}catch(l){throw l}}var A,j,w,U,x,S,z=[],I=0,E=[],G=Array(32768),J=0,L=!1,O=c.length,X=0,B=1,R=0,T=Array(288),q=Array(32),F=0,P=null,H=(Array(64),Array(64),0),M=Array(17),N=[];M[0]=0,S=function(){var e,r,n,t,o,i,a=[];if(8&A&&(a[0]=f(),a[1]=f(),a[2]=f(),a[3]=f(),80===a[0]&&75===a[1]&&7===a[2]&&8===a[3]?(e=f(),e|=f()<<8,e|=f()<<16,e|=f()<<24):e=a[0]|a[1]<<8|a[2]<<16|a[3]<<24,r=f(),r|=f()<<8,r|=f()<<16,r|=f()<<24,n=f(),n|=f()<<8,n|=f()<<16,n|=f()<<24),L&&v(),a[0]=f(),8===a[0]){if(A=f(),f(),f(),f(),f(),f(),t=f(),4&A)for(a[0]=f(),a[2]=f(),H=a[0]+256*a[1],o=0;H>o;o++)f();if(8&A)for(o=0,N=[],i=f();i;)("7"===i||":"===i)&&(o=0),u-1>o&&(N[o++]=i),i=f();if(16&A)for(i=f();i;)i=f();2&A&&(f(),f()),C(),e=f(),e|=f()<<8,e|=f()<<16,e|=f()<<24,n=f(),n|=f()<<8,n|=f()<<16,n|=f()<<24,L&&v()}},e.Util.Unzip.prototype.unzipFile=function(e){var r;for(this.unzip(),r=0;E.length>r;r++)if(E[r][1]===e)return E[r][0];return""},e.Util.Unzip.prototype.unzip=function(){return v(),E}},e.Util}),n("utils/encoding",["jxg"],function(e){var r=0,n=[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,8,8,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,10,3,3,3,3,3,3,3,3,3,3,3,3,4,3,3,11,6,6,6,5,8,8,8,8,8,8,8,8,8,8,8,0,12,24,36,60,96,84,12,12,12,48,72,12,12,12,12,12,12,12,12,12,12,12,12,12,0,12,12,12,12,12,0,12,0,12,12,12,24,12,12,12,12,12,24,12,24,12,12,12,12,12,12,12,12,12,24,12,12,12,12,12,24,12,12,12,12,12,12,12,24,12,12,12,12,12,12,12,12,12,36,12,36,12,12,12,36,12,12,12,12,12,36,12,36,12,12,12,36,12,12,12,12,12,12,12,12,12,12];return e.Util=e.Util||{},e.Util.UTF8={encode:function(e){var r,n,t="",o=e.length;if(e=e.replace(/\r\n/g,"\n"),"function"==typeof unescape&&"function"==typeof encodeURIComponent)return unescape(encodeURIComponent(e));for(r=0;o>r;r++)n=e.charCodeAt(r),128>n?t+=String.fromCharCode(n):n>127&&2048>n?(t+=String.fromCharCode(192|n>>6),t+=String.fromCharCode(128|63&n)):(t+=String.fromCharCode(224|n>>12),t+=String.fromCharCode(128|63&n>>6),t+=String.fromCharCode(128|63&n));return t},decode:function(e){var t,o,i,a=0,u=0,c=r,f=[],s=e.length,l=[];for(t=0;s>t;t++)o=e.charCodeAt(t),i=n[o],u=c!==r?63&o|u<<6:255>>i&o,c=n[256+c+i],c===r&&(u>65535?f.push(55232+(u>>10),56320+(1023&u)):f.push(u),a++,0===a%1e4&&(l.push(String.fromCharCode.apply(null,f)),f=[]));return l.push(String.fromCharCode.apply(null,f)),l.join("")},asciiCharCodeAt:function(e,r){var n=e.charCodeAt(r);if(n>255)switch(n){case 8364:n=128;break;case 8218:n=130;break;case 402:n=131;break;case 8222:n=132;break;case 8230:n=133;break;case 8224:n=134;break;case 8225:n=135;break;case 710:n=136;break;case 8240:n=137;break;case 352:n=138;break;case 8249:n=139;break;case 338:n=140;break;case 381:n=142;break;case 8216:n=145;break;case 8217:n=146;break;case 8220:n=147;break;case 8221:n=148;break;case 8226:n=149;break;case 8211:n=150;break;case 8212:n=151;break;case 732:n=152;break;case 8482:n=153;break;case 353:n=154;break;case 8250:n=155;break;case 339:n=156;break;case 382:n=158;break;case 376:n=159;break;default:}return n}},e.Util.UTF8}),n("utils/base64",["jxg","utils/encoding"],function(e,r){function n(e,r){return 255&e.charCodeAt(r)}function t(e,r){var n=o.indexOf(e.charAt(r));if(-1===n)throw Error("JSXGraph/utils/base64: Can't decode string (invalid character).");return n}var o="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/",i="=";return e.Util=e.Util||{},e.Util.Base64={encode:function(e){var t,a,u,c,f,s=[];for(f=r.encode(e),u=f.length,c=u%3,t=0;u-c>t;t+=3)a=n(f,t)<<16|n(f,t+1)<<8|n(f,t+2),s.push(o.charAt(a>>18),o.charAt(63&a>>12),o.charAt(63&a>>6),o.charAt(63&a));switch(c){case 1:a=n(f,u-1),s.push(o.charAt(a>>2),o.charAt(63&a<<4),i,i);break;case 2:a=n(f,u-2)<<8|n(f,u-1),s.push(o.charAt(a>>10),o.charAt(63&a>>4),o.charAt(63&a<<2),i)}return s.join("")},decode:function(e,n){var o,a,u,c,f,s,l=[],p=[];if(o=e.replace(/[^A-Za-z0-9\+\/=]/g,""),u=o.length,0!==u%4)throw Error("JSXGraph/utils/base64: Can't decode string (invalid input length).");for(o.charAt(u-1)===i&&(c=1,o.charAt(u-2)===i&&(c=2),u-=4),a=0;u>a;a+=4)f=t(o,a)<<18|t(o,a+1)<<12|t(o,a+2)<<6|t(o,a+3),p.push(f>>16,255&f>>8,255&f),0===a%1e4&&(l.push(String.fromCharCode.apply(null,p)),p=[]);switch(c){case 1:f=t(o,u)<<12|t(o,u+1)<<6|t(o,u+2),p.push(f>>10,255&f>>2);break;case 2:f=t(o,a)<<6|t(o,a+1),p.push(f>>4)}return l.push(String.fromCharCode.apply(null,p)),s=l.join(""),n&&(s=r.decode(s)),s},decodeAsArray:function(e){var r,n=this.decode(e),t=[],o=n.length;for(r=0;o>r;r++)t[r]=n.charCodeAt(r);return t}},e.Util.Base64}),n("../build/compressor.deps.js",["jxg","utils/zip","utils/base64"],function(e,r,n){return e.decompress=function(e){return unescape(new r.Unzip(n.decodeAsArray(e)).unzip()[0][0])},e}),window.JXG=r("../build/compressor.deps.js")})();

function updateSend(str, start){
	var end =  window.performance.now();
	var st = str.trim();
	var sp = st.split(':');
	var ansid = sp[0];

	// var decoded = Base64.decode(sp[1]);    // sp[1] contains Base64 encoded compressed bytes using GZip. See server source below:
	
	var decoded = JXG.decompress(sp[1]);
	/*	

		// Scala code used for compression
	  private def uncompress(s:String) = uncompressT(s, a => a).mkString
	  private def uncompressT[T](compressed:String, fromJson:String => Seq[T]) = {
		using(new GZIPInputStream(new ByteArrayInputStream(new sun.misc.BASE64Decoder().decodeBuffer(compressed)))){is =>
		  fromJson(scala.io.Source.fromInputStream(is).mkString)
		}
	  }  

	  private def compress(s:String) = compressT[Char](s, a => a.mkString)
  	  private def compressT[T](u:Seq[T], toJson:Seq[T] => String) = {
		val baos = new ByteArrayOutputStream
		using(new OutputStreamWriter(new GZIPOutputStream(baos))){osw =>
		  osw.write(toJson(u))
		}
		new sun.misc.BASE64Encoder().encode(baos.toByteArray)    
	  }
	*/
	


	var resp = "";
	try {
		  resp = JSON.parse(decoded);
	} catch (err) {
		  resp = decoded;
	}  
	// document.getElementById(ansid).innerHTML = 'result: <br>'+JSON.stringify(resp);
	var p = JSON.stringify(resp);
	var x = JSON.parse(p);
	var jsonHtmlTable = null;
	//if (document.getElementById(ansid).className == "file") {
	//	var y = '<a target="_blank" href="/getfile?fileID='+x+'">'+x+'</a>';
	//	jsonHtmlTable = y;
	//} else {
		jsonHtmlTable = ConvertJsonToTable(x, 'jsonTable', null, 'Download');
	//}
	document.getElementById(ansid).innerHTML = 'result: '+
			  '(<a href="javascript:void(null)" onclick="document.getElementById(\''+
			  ansid+'\').innerHTML = \'\'">clear</a>)'+' [timestamp: '+(Date.now())+';time elapsed: '+((end-start)/1000).toFixed(5)+' seconds]'+
			  '<br>'+jsonHtmlTable;
} 
function boolSelect(inputID, value) {
	document.getElementById(inputID).value = value;
}
function dateSelect(inputID, value) {
	var date = new Date(value);
        document.getElementById(inputID).value = date.getTime();
}
function toggleHide(selectorID, fieldID) {      
        var selectorState = document.getElementById(selectorID).style.display;
        var fieldState = document.getElementById(fieldID).style.display;
        document.getElementById(selectorID).style.display = selectorState === 'none' ? 'block' : 'none';
        document.getElementById(fieldID).style.display = fieldState === 'none' ? 'block' : 'none';
}
function getAddress() {
        return(document.links[0].href);
}
  // https://stackoverflow.com/a/197761/243233
  // https://stackoverflow.com/a/3871370/243233
function changeBackground(color) {
	// https://stackoverflow.com/a/14307259/243233
        document.body.style.background = color;
	var elements = document.getElementsByClassName('elegant-aero');
        for (var i = 0; i < elements.length; i++) {
 	   elements[i].style.backgroundColor=color;
        }
}
  
function changeBGIfLocalhost() {
    var string = getAddress();
    var substring = "localhost";
    var port = location.port;
    var bg = '#D3C6C6';
    if (port == 8080) bg = '#eae71e';
    // https://stackoverflow.com/a/1789952/243233
    if (string.indexOf(substring) !== -1){
        changeBackground(bg);	
    }
}
</script>
"""
}
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////