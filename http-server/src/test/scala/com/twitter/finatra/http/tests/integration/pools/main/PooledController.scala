package com.twitter.finatra.http.tests.integration.pools.main

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.tests.integration.pools.main.PooledController._
import com.twitter.finatra.utils.FuturePools

object PooledController {
  val pool1 = FuturePools.fixedPool("PooledController 1", 1)
  val pool2 = FuturePools.fixedPool("PooledController 2", 1)
}

class PooledController extends Controller {

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
