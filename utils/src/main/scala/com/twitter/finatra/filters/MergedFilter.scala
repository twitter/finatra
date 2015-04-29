package com.twitter.finatra.filters

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.util.Future

class MergedFilter[Req <: Request](
  filters: Filter[Req, Response, Req, Response]*)
  extends Filter[Req, Response, Req, Response] {

  private val CombinedFilter = filters reduceLeft {_ andThen _}

  def apply(request: Req, service: Service[Req, Response]): Future[Response] = {
    CombinedFilter(request, service)
  }
}
