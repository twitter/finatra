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



object App extends Logging {

  class FinatraService extends Service[Request, Response]{  
   def apply(request: Request) = {
      Router.dispatchAndReturn(request)
    }
  }


  def main(args : Array[String]) {
    //logger conf
    Logging.configure { log =>
      log.registerWithJMX = true

      log.level = Level.INFO

      log.console.enabled = true
      log.console.threshold = Level.WARN

      log.file.enabled = true
      log.file.filename = "logs/finatra.log"
      log.file.maxSize = 10 * 1024 // KB
      log.file.retainedFiles = 5 // keep five old logs around

    }

    log.info("starting server")
    log.info("reading configuration")

    //load the conf
    Config()

    //init stuff here
    val finatraService = new FinatraService
    
    val server: Server = ServerBuilder()
      .codec(RichHttp[Request](Http()))
      .bindTo(new InetSocketAddress(7070))
      .name("finatraService")
      .build(finatraService)
    
    log.info("server started on 7070")
    println("started on 7070: view logs/finatra.log for more info")
  }

}

