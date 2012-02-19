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

class Params {

}


class FinatraApp { 

    var request:Request = null

    def returnFuture(x:String) = {
      val response = Response(Http11, InternalServerError)
      response.mediaType = "text/plain" 
      response.content = copiedBuffer(x, UTF_8)
      Future.value(response)
    }

    def params(name:String):Option[String] = {
      this.request.params.get(name)
    }

    def apply(request:Request) = {
      this.request = request
      var res = "none"
      Router.routes.foreach( route => route match {
        case (method, pattern, callback) =>
          var thematch = pattern(request.uri).getOrElse(null)
          if(thematch != null)
            res = "matches"
      })
      returnFuture(res)
      //println(Router.routes)
      //returnFuture(Router.routes.get(key).getOrElse(null)().toString)
    }

}
