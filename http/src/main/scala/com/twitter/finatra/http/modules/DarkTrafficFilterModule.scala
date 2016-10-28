package com.twitter.finatra.http.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.finagle.exp.DarkTrafficFilter
import com.twitter.finagle.{Filter, Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.annotations.{CanonicalResourceFilter, DarkTrafficFilterType, DarkTrafficService}
import com.twitter.finatra.http.HttpHeaders
import com.twitter.finatra.http.contexts.RouteInfo
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
    @CanonicalResourceFilter canonicalResourceFilter: Filter[Request, Response, Request, Response],
    @DarkTrafficService darkService: Option[Service[Request, Response]]
  ): Filter[Request, Response, Request, Response] = {

    darkService match {
      case Some(service) =>
        val filteredDarkService = canonicalResourceFilter.andThen(service)
        new DarkTrafficFilter[Request, Response](
          filteredDarkService,
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

  /**
   * Provides a filter to add the Canonical-Resource header which is used by Diffy Proxy
   * @see [[https://github.com/twitter/diffy Diffy Project]]
   */
  @Provides
  @Singleton
  @CanonicalResourceFilter
  def provideCanonicalResourceFilter: Filter[Request, Response, Request, Response] = {
    Filter.mk[Request, Response, Request, Response] { (request, service) =>
      RouteInfo(request).foreach { info =>
        val nameOrPath = if (info.name.nonEmpty) {
          info.name
        } else {
          info.path
        }
        request.headerMap.set(HttpHeaders.CanonicalResource, s"${request.method.toString}_${nameOrPath}")
      }
      service(request)
    }
  }
}