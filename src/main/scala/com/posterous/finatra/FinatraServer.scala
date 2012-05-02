package com.posterous.finatra

import com.twitter.finagle.{Service, SimpleFilter}
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.util.Future
import java.net.InetSocketAddress
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.http.Http


import scala.collection.mutable.ListBuffer
import org.fusesource.scalate._
/**
 * @author ${user.name}
 */
import com.codahale.logula.Logging
import org.apache.log4j.Level



object FinatraServer extends Logging {

  class FinatraService extends Service[HttpRequest, HttpResponse]{
   def apply(request: HttpRequest) = {
      Router.dispatchAndReturn(request)
    }
  }

  var apps = ListBuffer[Function0[_]]()

  var docroot:String = "public"

  var templateEngine:TemplateEngine = new TemplateEngine()

  //def register(app: FinatraApp) { apps += (() => app) }

  def start(port:Int = 7070, docroot:String = "public") {
    this.docroot = docroot

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
    apps.foreach(app => app())

    //init stuff here
    val finatraService = new FinatraService
    val fileHandler = new FileHandler

    val service: Service[HttpRequest, HttpResponse] = fileHandler andThen finatraService

    val server: Server = ServerBuilder()
      .codec(Http())
      .bindTo(new InetSocketAddress(port))
      .name("finatraService")
      .build(service)

    log.info("server started on 7070")
    println("started on 7070: view logs/finatra.log for more info")
  }

}

