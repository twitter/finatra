package com.twitter.finatra.http.integration.doeverything.main

import com.twitter.finatra.http.routing.HttpWarmup
import com.twitter.finatra.httpclient.RequestBuilder._
import com.twitter.finatra.utils.Handler
import javax.inject.Inject

class DoEverythingWarmupHandler @Inject()(
  httpWarmup: HttpWarmup)
  extends Handler {

  override def handle() = {
    httpWarmup.send(
      get("/ok"))

    httpWarmup.send(
      post("/post"))

    httpWarmup.send(
      put("/put"))

    httpWarmup.send(
      delete("/delete"))

    httpWarmup.send(
      get("/admin/foo"))

    httpWarmup.send(
      get("/health"),
      forceRouteToAdminHttpMuxers = true)
  }
}