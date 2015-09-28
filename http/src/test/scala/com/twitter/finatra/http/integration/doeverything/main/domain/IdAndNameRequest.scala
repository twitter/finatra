package com.twitter.finatra.http.integration.doeverything.main.domain

import com.twitter.finagle.httpx.Request
import com.twitter.finatra.request.RouteParam
import javax.inject.Inject

case class IdAndNameRequest(
  @RouteParam id: Long,
  name: String)

case class IdRequest(
  @RouteParam id: Long,
  @Inject request: Request)

