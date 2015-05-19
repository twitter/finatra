package com.twitter.finatra.filters

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service}
import com.twitter.util.Future

class MergedFilter[R <: Request](
  filters: Filter[R, Response, R, Response]*)
  extends Filter[R, Response, R, Response] {

  private val CombinedFilter = filters reduceLeft {_ andThen _}

  def apply(request: R, service: Service[R, Response]): Future[Response] = {
    CombinedFilter(request, service)
  }
}
