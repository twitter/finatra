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
import com.codahale.logula.Logging
import org.apache.log4j.Level



object App {


  class FinatraService extends Service[Request, Response]{  
   def apply(request: Request) = {
      SomeShit(request)
      Whatever(request)
    }
  }


  def main(args : Array[String]) {
    val finatraService = new FinatraService
    Logging.configure { log =>
      log.registerWithJMX = true

      log.level = Level.INFO
      //log.loggers("com.posterous.weebits") = Level.OFF

      log.console.enabled = true
      log.console.threshold = Level.WARN

      log.file.enabled = true
      log.file.filename = "logs/finatra.log"
      log.file.maxSize = 10 * 1024 // KB
      log.file.retainedFiles = 5 // keep five old logs around

    }

    val server: Server = ServerBuilder()
      .codec(RichHttp[Request](Http()))
      .bindTo(new InetSocketAddress(7070))
      .name("finatraService")
      .build(finatraService)

    println("started on 7070")
  }

}

