package com.twitter.finatra.http.routing

import com.twitter.finagle.http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.FuturePools
import com.twitter.inject.Logging
import com.twitter.util.{Await, Future}
import javax.inject.Inject

class HttpWarmup @Inject()(
  router: HttpRouter,
  mapper: FinatraObjectMapper)
  extends Logging {

  private val userAgent = "http-warmup-client"

  /* Use a FuturePool to avoid getting a ConstFuture from Future.apply(...) */
  private val pool = FuturePools.fixedPool("HTTP Warmup", 1)

  /* Public */

  /**
   * Send a request to warmup services that are not yet externally receiving traffic.
   *
   * @param request - [[com.twitter.finagle.http.Request]] to send
   * @param forceRouteToAdminHttpMuxers - route the request to the HttpMuxer (e.g. needed for twitter-server admin
   *                                    endpoints that do not start with /admin)
   * @param times - number of times to send the request
   * @param responseCallback - callback called for every response where assertions can be made. NOTE: be aware that
   *                         failed assertions that throws Exceptions could prevent a server from restarting. This is
   *                         generally when dependent services are unresponsive causing the warm-up request(s) to fail.
   *                         As such, you should wrap your warm-up calls in these situations in a try/catch {}.
   */
  def send(
    request: => Request,
    forceRouteToAdminHttpMuxers: Boolean = false,
    times: Int = 1,
    responseCallback: Response => Unit = identity _) {

    for (i <- 1 to times) {
      val response = executeRequest(request, forceRouteToAdminHttpMuxers)
      responseCallback(response)
      info("Warmup " + request + " complete with " + response.status)
    }
  }

  def close() = {
    pool.executor.shutdownNow()
  }

  /* Private */

  private def executeRequest(request: Request, forceRouteToHttpMuxer: Boolean): Response = {
    val response = pool {
      info("Warmup " + request)
      routeRequest(request, forceRouteToHttpMuxer)
    }.flatten

    Await.result(response)
  }

  private def routeRequest(request: Request, forceRouteToHttpMuxer: Boolean): Future[Response] = {
    /* Mutation */
    request.headerMap.set("Host", "127.0.0.1")
    request.headerMap.set("User-Agent", userAgent)

    if (forceRouteToHttpMuxer)
      http.HttpMuxer(request)
    else if (request.uri.startsWith("/admin/finatra/"))
      router.services.adminService(request)
    else if (request.uri.startsWith("/admin"))
      http.HttpMuxer(request)
    else
      router.services.externalService(request)
  }
}
