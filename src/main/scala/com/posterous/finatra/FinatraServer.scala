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
import com.capotej.finatra_core._
import scala.collection.JavaConversions._

object FinatraServer {

  val controllers = new ControllerCollection
  val templateEngine = new TemplateHandler
  var docroot = "public"

  def register(app: FinatraApp) { controllers.add(app) }

  class FinatraService extends Service[HttpRequest, HttpResponse]{

    def pathOf(x:String) = x.split('?').first

    def paramsOf(request: HttpRequest) = {
      val fakeQs = "/?" + new String(request.getContent.array)
      val qs = new QueryStringDecoder(request.getUri)
      val bs = new QueryStringDecoder(fakeQs)
      var paramsHash = Map[String,String]()
      val allParams = qs.getParameters ++ bs.getParameters

      allParams.foreach { xs =>
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

    // log.info("starting server")
    // log.info("reading configuration")

    //init stuff here
    val finatraService = new FinatraService
    val fileHandler = new FileHandler

    val service: Service[HttpRequest, HttpResponse] = fileHandler andThen finatraService

    val server: Server = ServerBuilder()
      .codec(Http())
      .bindTo(new InetSocketAddress(port))
      .name("finatraService")
      .build(service)

    // log.info("server started on 7070")
    println("started on 7070: view logs/finatra.log for more info")
  }

}

