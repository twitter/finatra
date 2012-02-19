package com.posterous.finatra
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
import com.posterous.finatra.Router.{get,post,put,delete,patch,head}
import com.codahale.logula.Logging

class FinatraApp extends Logging { 

    var request:Request = null
    var paramsHash:Map[String, String] = Map()

    def returnFuture(x:String) = {
      val response = Response(Http11, InternalServerError)
      response.mediaType = "text/plain" 
      response.content = copiedBuffer(x, UTF_8)
      Future.value(response)
    }

    def loadUrlParams() {
      
      this.request.params.foreach(xs => this.paramsHash += xs)
    }

    def loadMatchParams(path:Option[Map[_, _]]) {
      path.foreach(xs => println(xs))
    }

    def params(name:String):Option[String] = {
      //this.paramsHash.get(name)
      this.request.params.get(name)
    }

    def apply(request: Request) = {
      returnFuture("a")
    }
    def lapply(request:Request) = {
      log.info("apply: recvd request %s", request.uri)
      this.paramsHash = Map()
      this.request = request
      loadUrlParams()
      var thematch:Option[Map[_,_]] = None
      val foundRoute = Router.routes.find( route => route match {
        case (method, pattern, callback) =>
          thematch = pattern(request.uri)
          if(thematch.getOrElse(null) != null) {
            loadMatchParams(thematch)
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
