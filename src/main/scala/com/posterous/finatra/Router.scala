package com.posterous.finatra

import scala.collection.mutable.HashSet
import java.net.InetSocketAddress
import java.util.{NoSuchElementException => NoSuchElement}
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import com.twitter.util.Future
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.finagle.http.{Http, RichHttp, Request, Response}
import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.Version.Http11
import com.twitter.finagle.http.path._
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.codahale.logula.Logging

object Router extends Logging {
  
  var routes: HashSet[(String, PathPattern, Function0[Any])] = HashSet()
  var request:Request = null
  var paramsHash:Map[String, String] = Map()


  def loadUrlParams() {
    this.request.params.foreach(xs => this.paramsHash += xs)
    log.info("paramsHash is %s", this.paramsHash) 
  }

  def parseMatchParam(xs: Tuple2[_, _]) = {
    log.info("  matching %s", xs._2)
   // val key = xs.getOrElse(null)
    //log.info("    key is %s", key)
  }

  def params(name:String):Option[String] = {
    //this.paramsHash.get(name)
    this.request.params.get(name)
  }


  def addRoute(method: String, path: String)(callback: => Any) {
    val regex = SinatraPathPatternParser(path)
    log.info("adding regex %s for %s", regex, path)
    routes += Tuple3(method, regex, (() => callback))
  }

  def returnFuture(x:String) = {
    val response = Response(Http11, InternalServerError)
    response.mediaType = "text/plain" 
    response.content = copiedBuffer(x, UTF_8)
    Future.value(response)
  }

  def dispatch(request: Request) = {
    log.info("apply: recvd request %s", request.uri)
    this.paramsHash = Map()
    this.request = request
    loadUrlParams()
    var thematch:Option[Map[_,_]] = None
    val foundRoute = this.routes.find( route => route match {
      case (method, pattern, callback) =>
        thematch = pattern(request.uri)
        if(thematch.getOrElse(null) != null) {
          thematch.getOrElse(null).foreach(xs => parseMatchParam(xs))
          true
        } else {
          false
        }
    })
    val result = foundRoute match {
      case Some((method, pattern,callback)) => callback()
      case None => "404"
    }
    returnFuture(result.toString)
    //println(Router.routes)
    //returnFuture(Router.routes.get(key).getOrElse(null)().toString)
  }

}
