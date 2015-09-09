package com.twitter.tiny.domain.http

import com.twitter.finagle.httpx.Request
import com.twitter.finatra.request.RequestInject

case class PostUrlRequest(
  @RequestInject request: Request,
  url: String)
