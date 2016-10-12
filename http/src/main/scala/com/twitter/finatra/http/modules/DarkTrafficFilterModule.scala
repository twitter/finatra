package com.twitter.finatra.http.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.finagle.exp.DarkTrafficFilter
import com.twitter.finagle.{Filter, Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.annotations.{DarkTrafficFilterType, DarkTrafficService}
import com.twitter.inject.TwitterModule
import com.twitter.util.Duration

abstract class DarkTrafficFilterModule extends TwitterModule {

  private val destFlag = flag[String]("http.dark.service.dest", "Resolvable name/dest of dark traffic service")

  /**
   * Forward the dark request after the service has processed the request
   * instead of concurrently.
   */
  val forwardAfterService: Boolean = true

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   */
  def enableSampling: Request => Boolean

  /**
   * Name of dark service client for use in metrics.
   */
  protected val label: String = "service"

  /**
   * Timeouts of the dark service client
   */
  protected val acquisitionTimeout: Duration
  protected val requestTimeout: Duration

  @Provides
  @Singleton
  @DarkTrafficFilterType
  def provideDarkTrafficFilter(
    statsReceiver: StatsReceiver,
    @DarkTrafficService darkService: Option[Service[Request, Response]]
  ): Filter[Request, Response, Request, Response] = {

    darkService match {
      case Some(service) =>
        new DarkTrafficFilter[Request, Response](
          service,
          enableSampling,
          statsReceiver,
          forwardAfterService)
      case _ => Filter.identity
    }
  }

  @Provides
  @Singleton
  @DarkTrafficService
  def provideDarkTrafficService(
    statsReceiver: StatsReceiver
  ): Option[Service[Request, Response]] = {

    destFlag.get match {
      case Some(dest) =>
        val clientStatsReceiver = statsReceiver.scope("clnt", "dark_traffic_filter")

        Some(
          Http.client
            .withSession.acquisitionTimeout(acquisitionTimeout)
            .withStatsReceiver(clientStatsReceiver)
            .withRequestTimeout(requestTimeout)
            .newService(dest, label))
      case _ => None
    }
  }
}