package com.twitter.finatra

import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer


class ErrorFilter extends SimpleFilter[FinagleRequest, FinagleResponse] with Logging {
  def apply(request: FinagleRequest, service: Service[FinagleRequest, FinagleResponse]) = {

    // `handle` asynchronously handles exceptions.
    service(request) handle { case error =>
      val statusCode = error match {
        case _: IllegalArgumentException =>
          FORBIDDEN
        case _ =>
          INTERNAL_SERVER_ERROR
      }
      val errorResponse = new DefaultHttpResponse(HTTP_1_1, statusCode)
      errorResponse.setContent(copiedBuffer("Internal Server Error".getBytes))
      logger.error(error, "Internal Server Error")

      FinagleResponse(errorResponse)
    }
  }
}
