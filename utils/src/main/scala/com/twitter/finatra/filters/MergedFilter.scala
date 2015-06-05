package com.twitter.finatra.filters

import com.twitter.finagle.{Filter, Service}
import com.twitter.util.Future

class MergedFilter[Req, Resp](
  filters: Filter[Req, Resp, Req, Resp]*)
  extends Filter[Req, Resp, Req, Resp] {

  private val CombinedFilter = filters reduceLeft {_ andThen _}

  def apply(request: Req, service: Service[Req, Resp]): Future[Resp] = {
    CombinedFilter(request, service)
  }
}
