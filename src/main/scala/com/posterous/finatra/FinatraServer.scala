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
/**
 * @author ${user.name}
 */
import com.codahale.logula.Logging
import org.apache.log4j.Level
import com.capotej.finatra_core._
import scala.collection.JavaConversions._
import org.fusesource.scalate._


object FinatraServer extends Logging {

  val controllers = new ControllerCollection
  var templateEngine:TemplateEngine = new TemplateEngine
  var docroot:String = "public"

  def register(app: FinatraApp) { controllers.add(app) }

  class FinatraService extends Service[HttpRequest, HttpResponse]{

    def pathOf(x:String) = x.split('?').first

    def paramsOf(request: HttpRequest) = {
      val qs = new QueryStringDecoder(request.getUri)
      var paramsHash = Map[String,String]()
      qs.getParameters.foreach { xs =>
        paramsHash += Tuple2(xs._1, xs._2.first)
      }
      paramsHash
    }

    def headersOf(request: HttpRequest) = {
      var headers = Map[String,String]()
      request.getHeaderNames.foreach { name =>
        headers += Tuple2(name, request.getHeader(name))
      }
      headers
    }

    def notFoundResponse = {
      val resp = new DefaultHttpResponse(HTTP_1_1, OK)
      resp.setContent(copiedBuffer("not found", UTF_8))
      Future.value(resp)
    }

    def apply(rawRequest: HttpRequest) = {
      val request = new FinatraRequest(path=pathOf(rawRequest.getUri()),
                                       method=rawRequest.getMethod.toString,
                                       params=paramsOf(rawRequest),
                                       headers=headersOf(rawRequest),
                                       body=(rawRequest.getContent.array))


      FinatraServer.controllers.dispatch(request) match {
        case Some(response) =>
          response.asInstanceOf[Future[HttpResponse]]
        case None =>
          notFoundResponse
      }
    }
  }

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

