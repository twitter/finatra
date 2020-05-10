package com.twitter.finatra.http.modules

import com.google.inject.Provides
import com.twitter.app.Flag
import com.twitter.finagle.exp.DarkTrafficFilter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{Filter, Http, Service}
import com.twitter.finatra.annotations.{
  CanonicalResourceFilter,
  DarkTrafficFilterType,
  DarkTrafficService
}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton

/* exposed for testing */
private[finatra] object DarkTrafficFilterModule {

  /**
   * HTTP `'''Canonical-Resource'''` header field name, used in Diffy Proxy
   * @see [[https://github.com/twitter/diffy/tree/master/example Diffy Project]]
   */
  val CanonicalResource = "Canonical-Resource"
}

abstract class DarkTrafficFilterModule extends TwitterModule {
  import DarkTrafficFilterModule._

  private val destFlag: Flag[String] =
    flag[String]("http.dark.service.dest", "Resolvable name/dest of dark traffic service")

  /**
   * Name of dark service client for use in metrics.
   */
  protected val label: String = "service"

  /**
   * Forward the dark request after the service has processed the request
   * instead of concurrently.
   */
  val forwardAfterService: Boolean = true

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   *
   * @param injector the [[com.twitter.inject.Injector]] for use in determining if a given request
   *                 should be forwarded or not.
   */
  def enableSampling(injector: Injector): Request => Boolean

  /**
   * Override to specify further configuration of the Finagle [[Http.Client]].
   *
   * @param injector the [[com.twitter.inject.Injector]] for use in configuring the underlying client.
   * @param client   the default configured [[Http.Client]].
   *
   * @return a configured instance of the [[Http.Client]]
   */
  protected def configureHttpClient(injector: Injector, client: Http.Client): Http.Client = client

  @Provides
  @Singleton
  @DarkTrafficFilterType
  final def provideDarkTrafficFilter(
    injector: Injector,
    statsReceiver: StatsReceiver,
    @CanonicalResourceFilter canonicalResourceFilter: Filter[Request, Response, Request, Response],
    @DarkTrafficService service: Option[Service[Request, Response]]
  ): Filter[Request, Response, Request, Response] = {
    service match {
      case Some(svc) =>
        val filteredDarkService = canonicalResourceFilter.andThen(svc)
        new DarkTrafficFilter[Request, Response](
          filteredDarkService,
          enableSampling(injector),
          statsReceiver,
          forwardAfterService
        )
      case _ =>
        Filter.identity
    }
  }

  @Provides
  @Singleton
  @DarkTrafficService
  final def provideDarkTrafficService(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): Option[Service[Request, Response]] = {

    destFlag.get.map { dest =>
      val clientStatsReceiver =
        statsReceiver.scope("clnt", "dark_traffic_filter")
      configureHttpClient(
        injector,
        defaultHttpClient(clientStatsReceiver)
      ).newService(dest, label)
    }
  }

  /**
   * Provides a filter to add the Canonical-Resource header which is used by Diffy Proxy
   * @see [[https://github.com/twitter/diffy Diffy Project]]
   */
  @Provides
  @Singleton
  @CanonicalResourceFilter
  final def provideCanonicalResourceFilter: Filter[Request, Response, Request, Response] = {
    Filter.mk[Request, Response, Request, Response] { (request, service) =>
      for (info <- RouteInfo(request)) {
        val nameOrPath =
          if (info.name.nonEmpty) info.name
          else info.path
        request.headerMap
          .set(CanonicalResource, s"${request.method.toString}_$nameOrPath")
      }
      service(request)
    }
  }

  /* Private */

  private[this] def defaultHttpClient(statsReceiver: StatsReceiver): Http.Client =
    Http.client
      .withStatsReceiver(statsReceiver)
}
