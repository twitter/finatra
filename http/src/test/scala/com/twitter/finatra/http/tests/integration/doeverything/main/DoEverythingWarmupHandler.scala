package com.twitter.finatra.http.tests.integration.doeverything.main

import com.twitter.finatra.http.response.ResponseUtils._
import com.twitter.finatra.http.routing.HttpWarmup
import com.twitter.finatra.httpclient.RequestBuilder._
import com.twitter.inject.Logging
import com.twitter.inject.utils.Handler
import javax.inject.Inject
import scala.util.control.NonFatal

class DoEverythingWarmupHandler @Inject() (warmup: HttpWarmup) extends Handler with Logging {

  override def handle(): Unit = {
    try {
      warmup.send(get("/ok"))()

      warmup.send(post("/post"))(expectOkResponse)

      warmup.send(put("/put"))()

      warmup.send(delete("/delete"))()

      /* user-defined admin route should be available in warmup */
      warmup.send(get("/admin/testme"), admin = true)(expectOkResponse)

      // should not have to specify "admin = true" here.
      warmup.send(get("/admin/finatra/internal/route"))(expectOkResponse)

      /* TwitterServer HTTP Admin Interface admin routes are not routable via the HttpWarmup utility */
      warmup.send(get("/health"), admin = true)(expectNotFoundResponse)
    } catch {
      case e: java.lang.AssertionError =>
        throw e // any assertion error will fail the server startup
      case NonFatal(e) =>
        // we don't want failed warmup to prevent the server from starting
        error(e.getMessage, e)
    }
    info("Warm up done.")
  }
}
