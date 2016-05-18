package com.twitter.finatra.http.tests.integration.doeverything.main

import com.twitter.finatra.http.routing.HttpWarmup
import com.twitter.finatra.httpclient.RequestBuilder._
import com.twitter.inject.Logging
import com.twitter.inject.utils.Handler
import javax.inject.{Inject, Singleton}

@Singleton
class DoEverythingWarmupHandler @Inject()(
  warmup: HttpWarmup)
  extends Handler
  with Logging {

  override def handle(): Unit = {
    try {
      warmup.send(
        get("/ok"))

      warmup.send(
        post("/post"))

      warmup.send(
        put("/put"))

      warmup.send(
        delete("/delete"))

      warmup.send(
        get("/admin/finatra/foo"))

      warmup.send(
        get("/health"),
        forceRouteToAdminHttpMuxers = true)
    } catch {
      case e: Throwable =>
        // we don't want failed warmup to prevent the server from starting
        error(e.getMessage, e)
    } finally {
      warmup.close()
    }
    info("Warm up done.")
  }
}
