package com.twitter.finatra.http.tests.integration.darktraffic.main

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http
import com.twitter.finagle.http.Method.{Delete, Post}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.modules.DarkTrafficFilterModule
import com.twitter.inject.Injector

object DarkServerTestModule extends DarkTrafficFilterModule {

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   */
  override def enableSampling(injector: Injector): Request => Boolean = { request =>
    request.method match {
      case Delete => false
      case _ => true
    }
  }

  /**
   * Override to specify further configuration of the Finagle [[Http.Client]].
   *
   * @param injector the [[com.twitter.inject.Injector]] for use in configuring the underlying client.
   * @param client   the default configured [[Http.Client]].
   *
   * @return a configured instance of the [[Http.Client]]
   */
  override protected def configureHttpClient(
    injector: Injector,
    client: Http.Client
  ): Http.Client = {
    client
      .withSession.acquisitionTimeout(100.millis)
      .withRequestTimeout(100.millis)
  }
}
