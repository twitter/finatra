package com.posterous.finatra

import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.util.Future
import com.twitter.finagle.http.Http

import com.capotej.finatra_core._

class FinatraApp extends FinatraController {

  def response(status:Int = 200, body: String, headers: Map[String,String] = Map()) = {
    val responseStatus = HttpResponseStatus.valueOf(status)
    val resp = new DefaultHttpResponse(HTTP_1_1, responseStatus)
    resp.setContent(copiedBuffer(body, UTF_8))
    Future.value(resp)
  }

}