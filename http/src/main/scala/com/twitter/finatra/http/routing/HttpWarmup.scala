package com.twitter.finatra.http.routing

import com.twitter.finagle.http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.Logging
import com.twitter.util.{Await, Future}
import javax.inject.Inject

class HttpWarmup @Inject()(router: HttpRouter, mapper: FinatraObjectMapper) extends Logging {

  private val userAgent = "http-warmup-client"

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
    responseCallback: Response => Unit = identity _
  ): Unit = {

    for (_ <- 1 to times) {
      infoResult("%s") {
        val response = executeRequest(request, forceRouteToAdminHttpMuxers)
        responseCallback(response)
        s"Warmup $request completed with ${response.status}"
      }
    }
  }

  @deprecated("This is now a no-op.", "2018-03-20")
  def close(): Unit = {}

  /* Private */

  private def executeRequest(request: Request, forceRouteToHttpMuxer: Boolean): Response = {
    Await.result(routeRequest(request, forceRouteToHttpMuxer))
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
