package com.posterous.finatra

import com.twitter.finagle.{Service, SimpleFilter}
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.util.Future
import java.net.InetSocketAddress
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.http.Http
import scala.collection.JavaConversions._

import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import com.codahale.logula.Logging
import org.fusesource.scalate._
import java.io._


object Router extends Logging {
  case class FinatraResponse(var status: Int = 200, var mediaType: String = "text/html", var headers:ListBuffer[Tuple2[String,String]] = new ListBuffer[Tuple2[String,String]])
  
  var routes: HashSet[(String, PathPattern, Function0[Any])] = HashSet()
  var templateBindings:Map[String, Any] = Map()
  var request:HttpRequest = null
  var paramsHash:Map[String, String] = Map()
  var multiParams:Map[String, MultipartItem] = Map()
  var response:FinatraResponse = FinatraResponse()

  def bindTemplateVar(x:String, y:Any){
    templateBindings += Tuple2(x, y) 
  }

  def renderTemplate(path: String):String = {
    log.info("bindings are %s", templateBindings)
    log.info("rendering template %s", path)
    val st = System.currentTimeMillis
    val tfile = new File("templates", path) 
    val buffer = new StringWriter
    var str = ""
    try {
      val template = FinatraServer.templateEngine.layout(tfile.toString, templateBindings) 
      str = template
      //val context = new DefaultRenderContext(this.request.getUri, FinatraServer.templateEngine, new PrintWriter(buffer))
      //template.render(context)
      //str = buffer.toString
      val et = System.currentTimeMillis
      val time = et - st
      log.info("rendered template %s in %sms", path, time)
    } catch {
      case e: Exception => println(e)
    }
    str
  }

  def loadUrlParams() {
    val qs = new QueryStringDecoder(this.request.getUri);
    qs.getParameters.foreach(xs => this.paramsHash += Tuple2(xs._1, xs._2.first))
  }

  def parseMatchParam(xs: Tuple2[_, _]) = {
    this.paramsHash += Tuple2(xs._1.toString, xs._2.asInstanceOf[ListBuffer[String]].head.toString)
  }

  def params(name:String):String = {
    this.paramsHash.get(name) match {
      case Some(str) => str
      case None => ""
    }
  }

  def multiPart(name:String):MultipartItem = {
    this.multiParams.get(name) match {
      case Some(item) => item
      case None => null
    }
  }
  def addRoute(method: String, path: String)(callback: => Any) {
    val regex = SinatraPathPatternParser(path)
    routes += Tuple3(method, regex, (() => callback))
  }

  def returnFuture(response: HttpResponse) = {
    log.info("returning response %s", response)
    Future.value(response)
  }

  def routeExists(request: HttpRequest) = {
    var thematch:Option[Map[_,_]] = None
    
    this.routes.find( route => route match {
      case (method, pattern, callback) =>
        thematch = pattern(request.getUri)
        if(thematch.getOrElse(null) != null && method == request.getMethod.toString) {
          thematch.getOrElse(null).foreach(xs => parseMatchParam(xs))
          true
        } else {
          false
        }
    })
  }

  def buildResponse(output:String) = {
    val resp = new DefaultHttpResponse(HTTP_1_1, OK)
    resp.setStatus(HttpResponseStatus.valueOf(this.response.status))
    resp.setHeader("Content-Type", this.response.mediaType)
    this.response.headers.foreach(xs => resp.setHeader(xs._1, xs._2))
    resp.setContent(copiedBuffer(output.toString, UTF_8))

    resp
  }

  def setStatus(status:Int) = {
    this.response.status = status  
  }

  def dispatch(request: HttpRequest):HttpResponse = {
    log.info("recvd request: %s %s %s", request.getMethod, request.getUri, request.getHeaders)

    this.paramsHash = Map()
    this.templateBindings = Map()
    this.request    = request
    this.response   = FinatraResponse()
    this.multiParams = MultipartParsing.loadMultiParams(request)
    
    loadUrlParams()

    val result = this.routeExists(request) match {
      case Some((method, pattern,callback)) => callback()
      case none => 
        request.setMethod(GET)
        this.routeExists(request) match {
          case Some((method, patterh, callback)) => callback()
          case none =>
            setStatus(404)
            "404 Not Found"
        }
    }
    
    buildResponse(result.toString)
  }
  
  def dispatchAndReturn(request: HttpRequest) = {
    returnFuture(dispatch(request))
  }

}
