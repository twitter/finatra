package com.twitter.finatra.filters

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.util.Future

class MergedFilter(
  filters: Filter[Request, Response, Request, Response]*)
  extends Filter[Request, Response, Request, Response] {

  private val CombinedFilter = filters reduceLeft {_ andThen _}

  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    CombinedFilter(request, service)
  }
}
