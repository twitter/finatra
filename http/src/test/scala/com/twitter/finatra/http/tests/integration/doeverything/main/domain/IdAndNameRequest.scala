package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finagle.http.Request
import com.twitter.finatra.request.{JsonIgnoreBody, RouteParam}

case class IdAndNameRequest(
  @RouteParam id: Long,
  name: String)

case class IdRequest(
  @RouteParam id: Long,
  request: Request)

@JsonIgnoreBody
case class IdRequestIgnoringBody(
  @RouteParam id: Long)

case class IdRequestNotIgnoringBody(
  @RouteParam id: Long)
