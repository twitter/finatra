package com.twitter.finatra

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.{Time, Future}
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import com.twitter.app.App

class LoggingFilter extends SimpleFilter[FinagleRequest, FinagleResponse] with App with Logging  {
  def apply(request: FinagleRequest, service: Service[FinagleRequest, FinagleResponse]): Future[FinagleResponse] = {
    val start = System.currentTimeMillis()
    service(request) map { response =>
      val end = System.currentTimeMillis()
      val duration = end - start
      log.info("%s %s %d %dms".format(request.method, request.uri, response.statusCode, duration))
      response
    }
  }
}
