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
/**
 * @author ${user.name}
 */
object App {

  class HelloWorld extends Service[Request, Response]{  

    var routes: Map[String, Function0[Any]] = Map()

    def returnFuture(x:String) = {
      val response = Response(Http11, InternalServerError)
      response.mediaType = "text/plain" 
      response.content = copiedBuffer(x, UTF_8)
      Future.value(response)
    }

    def get(path: String)(callback: => Any) { 
      routes += path -> (() => callback)
    } 

    get("/") { returnFuture("lol") }

    def apply(request: Request) = {
      println(routes)
      returnFuture("Asd")
    }
  }


  def main(args : Array[String]) {
    val helloworld = new HelloWorld

    val server: Server = ServerBuilder()
      .codec(RichHttp[Request](Http()))
      .bindTo(new InetSocketAddress(7070))
      .name("helloworld")
      .build(helloworld)

    println("started on 7070")
  }

}

