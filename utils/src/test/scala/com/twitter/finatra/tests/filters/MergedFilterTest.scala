package com.twitter.finatra.tests.filters

import com.twitter.finagle.{Filter, Service, SimpleFilter}
import com.twitter.finagle.http.{Status, RequestBuilder, Response, Request}
import com.twitter.finatra.filters.MergedFilter
import com.twitter.inject.Test
import com.twitter.util.{Await, Future}

class MergedFilterTest extends Test {
  class AppendToHeaderFilter(header: String, value: String) extends SimpleFilter[Request, Response] {
    def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      val oldValue = request.headerMap.getOrElse(header, "")
      request.headerMap.update(header, oldValue + value)
      service(request)
    }
  }

  "MergedFilter" should {
    "merge filters" in {
      val merged = new MergedFilter(
        Filter.identity[Request, Response],
        Filter.identity[Request, Response],
        new AppendToHeaderFilter("test", "3"),
        new AppendToHeaderFilter("test", "4"),
        new AppendToHeaderFilter("test", "5"),
        Filter.identity[Request, Response],
        Filter.identity[Request, Response])

      val request =
        RequestBuilder
          .create()
          .url("http://127.0.0.1")
          .buildGet()

      val service = new Service[Request, Response] {
        override def apply(request: Request): Future[Response] = {
          val response = Response(Status.Ok)
          response.setContentString(request.headerMap("test"))
          Future.value(response)
        }
      }

      val response = Await.result {
        merged(request, service)
      }

      response.contentString should be("345")
    }
  }

}