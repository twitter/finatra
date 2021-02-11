package com.twitter.finatra.http.tests.integration.doeverything.main.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.routing.HttpForward

class MaxForwardController(maxDepth: Int, forward: HttpForward) extends Controller {

  def this(forward: HttpForward) =
    this(maxDepth = Integer.MAX_VALUE, forward = forward)

  get("/max") { request: Request =>
    request.ctx.apply(HttpForward.DepthField) match {
      case Some(depth) =>
        if (depth < maxDepth) {
          forward(request, "/helper/max")
        } else if (depth == maxDepth) {
          response.ok
        }
      case _ =>
        forward(request, "/helper/max")
    }
  }

  get("/infinite") { request: Request =>
    forward(request, "/helper/infinity")
  }

  get("/max/ok") { _: Request =>
    response.ok
  }
}
