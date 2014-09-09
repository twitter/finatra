package com.twitter.finatra.integration.internal

import com.twitter.finagle.http.Status._
import com.twitter.finatra.twitterserver.{Handler, HttpAssertions}
import javax.inject.Inject

class DoEverythingWarmupHandler @Inject()(
  httpAsserter: HttpAssertions)
  extends Handler {

  override def handle() = {
    httpAsserter.get("/ok", andExpect = Ok, withBody = "ok")

    //we haven't started until warmup completes, so an empty body should be returned from the health check
    httpAsserter.get("/health", routeToAdminServer = true, andExpect = Ok, withBody = "")
  }
}