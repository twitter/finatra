package com.posterous.finatra

import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.util.Future
import com.twitter.finagle.http.Http

import com.codahale.jerkson.Json._
import java.io._
import com.capotej.finatra_core._

class FinatraApp extends FinatraController {

  def render(status:Int = 200, path: String, layout: String = "application.mustache", exports: Any = "") = {
    val resp = FinatraServer.templateEngine.captureTemplate(path, layout, exports)
    response(body=resp, headers=Map("Content-Type" -> "text/html"))
  }

  def redirect(url: String) = {
    response(status=301, body="moved", headers=Map("Location" -> url))
  }


  // TODO abstract out both of these methods into a response object

  // def response(status:Int = 200, body: String, headers: Map[String,String] = Map(), cookies: Map[String, FinatraCookie] = Map()) = {
  //   val responseStatus = HttpResponseStatus.valueOf(status)
  //   val resp = new DefaultHttpResponse(HTTP_1_1, responseStatus)
  //   headers.foreach { xs =>
  //     resp.setHeader(xs._1, xs._2)
  //   }
  //   if (!cookies.isEmpty) {
  //     resp.setHeader("Cookie", FinatraCookieAdapter.out(cookies))
  //   }
  //   resp.setContent(copiedBuffer(body, UTF_8))
  //   Future.value(resp)
  // }

  def response(status:Int = 200, body: String, headers: Map[String,String] = Map(), cookies: Map[String, String] = Map()) = {
    val responseStatus = HttpResponseStatus.valueOf(status)
    val resp = new DefaultHttpResponse(HTTP_1_1, responseStatus)
    headers.foreach { xs =>
      resp.setHeader(xs._1, xs._2)
    }
    if (!cookies.isEmpty) {
      var finatraCookies = Map[String, FinatraCookie]()
      cookies.foreach { xs =>
        finatraCookies += (xs._1 -> FinatraCookie(xs._1, xs._2))
      }
      resp.setHeader("Cookie", FinatraCookieAdapter.out(finatraCookies))
    }
    resp.setContent(copiedBuffer(body, UTF_8))
    Future.value(resp)
  }

  def rawResponse(status:Int = 200, body: Array[Byte], headers: Map[String,String] = Map()) = {
    val responseStatus = HttpResponseStatus.valueOf(status)
    val resp = new DefaultHttpResponse(HTTP_1_1, responseStatus)
    headers.foreach { xs =>
      resp.setHeader(xs._1, xs._2)
    }
    resp.setContent(copiedBuffer(body))
    Future.value(resp)
  }

  def toJson(obj: Any) = {
    response(body=generate(obj), headers=Map("Content-Type" -> "application/json"))
  }

}