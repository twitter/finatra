package com.twitter.finatra.http.integration.doeverything.main.domain

import javax.inject.Inject

import com.twitter.finagle.httpx.Request
import com.twitter.finatra.request.{JsonIgnoreBody, RouteParam}

case class IdAndNameRequest(
  @RouteParam id: Long,
  name: String)

case class IdRequest(
  @RouteParam id: Long,
  @Inject request: Request)

@JsonIgnoreBody
case class IdRequestIgnoringBody(
  @RouteParam id: Long)

case class IdRequestNotIgnoringBody(
  @RouteParam id: Long)
