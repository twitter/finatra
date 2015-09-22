package com.twitter.finatra.http.routing

import com.twitter.finagle.httpx
import com.twitter.finagle.httpx.{Request, Response, Status}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.FuturePools
import com.twitter.inject.Logging
import com.twitter.util.{Await, ExecutorServiceFuturePool, Future}
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
   * Send a request to warmup services that are not yet externally receiving traffic
   * @param request Request to send
   * @param forceRouteToAdminHttpMuxers Route the request to the HttpMuxer (e.g. needed for twitter-server admin endpoints that do not start with /admin)
   * @param responseCallback Callback called for every response where assertions can be made (note: be aware that failed assertions will prevent a server from
   *                         restarting when dependent services are unresponsive)
   */
  def send(
    request: => Request,
    forceRouteToAdminHttpMuxers: Boolean = false,
    times: Int = 1,
    responseCallback: Response => Unit = identity) {

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
    request.headerMap.add("Host", "127.0.0.1")
    request.headerMap.add("User-Agent", userAgent)

    if (forceRouteToHttpMuxer)
      httpx.HttpMuxer(request)
    else if (request.uri.startsWith("/admin/finatra/"))
      router.services.adminService(request)
    else if (request.uri.startsWith("/admin"))
      httpx.HttpMuxer(request)
    else
      router.services.externalService(request)
  }
}
