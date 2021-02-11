package com.twitter.finatra.http.tests.integration.doeverything.main.filters

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future

class AppendToHeaderFilter(header: String, value: String) extends SimpleFilter[Request, Response] {
  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    val oldValue = request.headerMap.getOrElse(header, "")
    request.headerMap.update(header, oldValue + value)
    service(request)
  }
}
