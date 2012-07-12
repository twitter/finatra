package com.twitter.finatra

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.util.CharsetUtil.UTF_8


class AppService extends Service[HttpRequest, HttpResponse]{

  def notFoundResponse = {
    val resp = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
    resp.setContent(copiedBuffer("not found", UTF_8))
    Future.value(resp)
  }

  def apply(rawRequest: HttpRequest) = {
    val request = RequestAdapter(rawRequest)

    FinatraServer.controllers.dispatch(request) match {
      case Some(response) =>
        response.asInstanceOf[Future[HttpResponse]]
      case None =>
        notFoundResponse
    }
  }

}
