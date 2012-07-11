package com.twitter.finatra

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.{Cookie => JCookie}
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.util.CharsetUtil.UTF_8
import org.jboss.netty.handler.codec.http.HttpResponseStatus._

import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer


class AppService extends Service[HttpRequest, HttpResponse]{

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

  def cookiesOf(request: HttpRequest) = {
    var cookies = Map[String, Cookie]()
    // val cookie = request.getHeader("Cookie")
    // if(cookie != null){
    //   val decodedCookie = new CookieDecoder().decode(cookie)
    //   decodedCookie.foreach { xs =>
    //     cookies += Tuple2(xs.getName, CookieAdapter.in(xs))
    //   }
    // }
    cookies
  }

  def notFoundResponse = {
    val resp = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
    resp.setContent(copiedBuffer("not found", UTF_8))
    Future.value(resp)
  }

  def apply(rawRequest: HttpRequest) = {
    val request = new Request
    request.path    (pathOf(rawRequest.getUri))
    request.method  (rawRequest.getMethod.toString)
    request.params  (paramsOf(rawRequest))
    request.headers (headersOf(rawRequest))
    //request.cookies (cookiesOf(rawRequest))
    request.body    (rawRequest.getContent.array)

    FinatraServer.controllers.dispatch(request) match {
      case Some(response) =>
        response.asInstanceOf[Future[HttpResponse]]
      case None =>
        notFoundResponse
    }
  }
}
