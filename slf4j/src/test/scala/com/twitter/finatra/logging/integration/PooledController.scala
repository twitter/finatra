package com.twitter.finatra.logging.integration

import com.twitter.finagle.httpx.Request
import com.twitter.finatra.http.Controller

import com.twitter.util.FuturePool


class PooledController extends Controller {

  private val pool1 = FuturePool.unboundedPool
  private val pool2 = FuturePool.unboundedPool

  get("/hi") { request: Request =>
    val id = request.params("id")
    info("Handling request " + id)
    pool1 {
      Thread.sleep(2000)
      pool2 {
        for (i <- 1 to 5) {
          Thread.sleep(100)
          info("Hi " + id + "-" + i)
        }
      }
    }

    response.ok.body("Hello " + id)
  }
}
