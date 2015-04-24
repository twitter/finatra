package com.twitter.finatra.integration.doeverything.main

import com.twitter.finagle.http.Status._
import com.twitter.finatra.routing.HttpAssertions
import com.twitter.finatra.utils.Handler
import javax.inject.Inject

class DoEverythingWarmupHandler @Inject()(
  httpAsserter: HttpAssertions)
  extends Handler {

  override def handle() = {
    httpAsserter.get(
      "/ok",
      andExpect = Ok,
      withBody = "ok")

    httpAsserter.post(
      "/post",
      body = "postit",
      andExpect = Ok,
      withBody = "post")

    httpAsserter.put(
      "/put",
      body = "putit",
      andExpect = Ok,
      withBody = "putit")

    httpAsserter.delete(
      "/delete",
      andExpect = Ok,
      withBody = "delete")

    httpAsserter.get(
      "/admin/finatra/foo",
      andExpect = Ok,
      withBody = "bar")

    //we haven't started until warmup completes, so an empty body should be returned from the health check
    httpAsserter.get(
      "/health",
      routeToAdminServer = true,
      andExpect = Ok,
      withBody = "")
  }
}