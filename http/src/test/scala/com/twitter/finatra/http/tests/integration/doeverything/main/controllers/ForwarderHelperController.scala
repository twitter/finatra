package com.twitter.finatra.http.tests.integration.doeverything.main.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.request.HttpForward
import com.twitter.finatra.http.request.HttpForward.DepthField

class ForwarderHelperController(maxDepth: Int, forward: HttpForward) extends Controller {

  def this(forward: HttpForward) =
    this(maxDepth = Integer.MAX_VALUE, forward = forward)

  get("/helper/max") { request: Request =>
    request.ctx.apply(DepthField) match {
      case Some(depth) =>
        if (depth < maxDepth) {
          forward(request, "/max")
        } else if (depth == maxDepth) {
          response.ok
        }
      case _ =>
        forward(request, "/max")
    }
  }

  get("/helper/infinity") { request: Request =>
    forward(request, "/infinite")
  }

  get("/helper/ok") { request: Request =>
    response.ok
  }
}
