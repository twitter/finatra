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

  def captureTemplate(path: String, templateBinding: Map[String, String] = Map()): String = {
    val tfile = new File("templates", path)
    val buffer = new StringWriter
    var template = ""
    try {
      template = FinatraServer.templateEngine.layout(tfile.toString, templateBinding)
    } catch {
        case e: Exception => template = e.toString
    }
    template
  }

  def render(status:Int = 200, path: String, exports: Map[String, String] = Map()) = {
    val resp = captureTemplate(path, exports)
    response(body=resp, headers=Map("Content-Type" -> "text/html"))
  }

  def redirect(url: String) = {
    response(status=301, body="moved", headers=Map("Location" -> url))
  }

  def response(status:Int = 200, body: String, headers: Map[String,String] = Map()) = {
    val responseStatus = HttpResponseStatus.valueOf(status)
    val resp = new DefaultHttpResponse(HTTP_1_1, responseStatus)
    resp.setContent(copiedBuffer(body, UTF_8))
    Future.value(resp)
  }

  def toJson(obj: Any) = { response(body=generate(obj), headers=Map("Content-Type" -> "application/json")) }


}