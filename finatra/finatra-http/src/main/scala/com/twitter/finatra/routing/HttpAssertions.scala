package com.twitter.finatra.routing

import com.twitter.finagle.http._
import com.twitter.finatra.utils.{FuturePoolUtils, Logging}
import com.twitter.util.{Await, ExecutorServiceFuturePool, Future}
import javax.inject.Inject
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpResponseStatus}

class HttpAssertions @Inject()(
  router: Router)
  extends Logging {

  /* Use a "future pool" to start the first Future in the "Future chain" */
  private val pool = FuturePoolUtils.fixedPool("HTTP Warmup", 1).asInstanceOf[ExecutorServiceFuturePool]

  /* Public */

  def get(uri: String, andExpect: HttpResponseStatus = null, withBody: String = null, routeToAdminServer: Boolean = false) = {
    send(
      Request(uri),
      andExpect,
      withBody,
      routeToAdminServer)
  }

  def post(uri: String, expectedStatus: HttpResponseStatus = null, expectedBody: String = null, routeToAdminServer: Boolean = false) = {
    send(
      Request(Method.Post, uri),
      expectedStatus,
      expectedBody,
      routeToAdminServer)
  }

  def put(uri: String, expectedStatus: HttpResponseStatus = null, expectedBody: String = null, routeToAdminServer: Boolean = false) = {
    send(
      Request(Method.Put, uri),
      expectedStatus,
      expectedBody,
      routeToAdminServer)
  }

  def delete(uri: String, expectedStatus: HttpResponseStatus = null, expectedBody: String = null, routeToAdminServer: Boolean = false) = {
    send(
      Request(Method.Delete, uri),
      expectedStatus,
      expectedBody,
      routeToAdminServer)
  }

  def close() = {
    pool.executor.shutdownNow()
  }

  /* Private */

  private def send(request: Request, expectedStatus: HttpResponseStatus, expectedBody: String, routeToAdminServer: Boolean) {
    val nettyResponseFuture = pool {
      executeRequest(request, routeToAdminServer)
    }.flatten

    val finagleResponse = Response(
      Await.result(nettyResponseFuture))

    assert(
      expectedStatus == null || finagleResponse.status == expectedStatus,
      "Sent " + request + " and expected " + expectedStatus + " but received " + finagleResponse.status)

    assert(
      expectedBody == null || finagleResponse.contentString == expectedBody,
      "Sent " + request + " and expected body " + expectedBody + " but received \"" + finagleResponse.contentString + "\"")

    info("Sent " + request + " and received " + finagleResponse.status)
  }

  // Check Finatra's Admin RoutingController before checking the global HttpMuxer
  // We have to check both, since we don't add our Admin RoutingController to HttpMuxer until after warmup
  private def executeRequest(request: Request, routeToAdminServer: Boolean): Future[HttpResponse] = {
    request.headers().set("Host", "127.0.0.1") /* Mutation */

    if (request.uri.startsWith("/admin") || routeToAdminServer)
      executeAdminRequest(request)
    else
      router.services.externalService(request)
  }

  private def executeAdminRequest(request: Request): Future[HttpResponse] = {
    router.services.adminService(request) flatMap { response =>
      if (isRoutingServiceNotFound(request, response))
        HttpMuxer(request)
      else
        Future.value(response)
    }
  }


  private def isRoutingServiceNotFound(request: Request, response: Response): Boolean = {
    response.status == Status.NotFound &&
      (response.contentString endsWith RoutingService.NotFoundSuffix) //Hack :-/
  }
}
