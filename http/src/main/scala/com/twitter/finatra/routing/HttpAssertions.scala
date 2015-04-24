package com.twitter.finatra.routing

import com.twitter.finagle.http._
import com.twitter.finatra.utils.FuturePools
import com.twitter.inject.Logging
import com.twitter.util.{Await, ExecutorServiceFuturePool, Future}
import javax.inject.Inject
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpResponseStatus}

//TODO: Add additional HTTP methods
class HttpAssertions @Inject()(
  router: HttpRouter)
  extends Logging {

  /* Use a FuturePool to avoid getting a ConstFuture from Future.apply(...) */
  private val pool = FuturePools.fixedPool("HTTP Warmup", 1).asInstanceOf[ExecutorServiceFuturePool]

  /* Public */

  def get(uri: String, andExpect: HttpResponseStatus = null, withBody: String = null, routeToAdminServer: Boolean = false) = {
    send(
      Request(uri),
      andExpect,
      withBody,
      routeToAdminServer)
  }

  def post(uri: String, body: String, andExpect: HttpResponseStatus = null, withBody: String = null, routeToAdminServer: Boolean = false) = {
    val request = Request(Method.Post, uri)
    request.setContentString(body)

    send(
      request,
      andExpect,
      withBody,
      routeToAdminServer)
  }

  def put(uri: String, body: String, andExpect: HttpResponseStatus = null, withBody: String = null, routeToAdminServer: Boolean = false) = {
    val request = Request(Method.Put, uri)
    request.setContentString(body)

    send(
      request,
      andExpect,
      withBody,
      routeToAdminServer)
  }

  def delete(uri: String, andExpect: HttpResponseStatus = null, withBody: String = null, routeToAdminServer: Boolean = false) = {
    send(
      Request(Method.Delete, uri),
      andExpect,
      withBody,
      routeToAdminServer)
  }

  def close() = {
    pool.executor.shutdownNow()
  }

  /* Private */

  private def send(request: Request, expectedStatus: HttpResponseStatus, withBody: String, routeToAdminServer: Boolean) {
    val nettyResponseFuture = pool {
      executeRequest(request, routeToAdminServer)
    }.flatten

    val finagleResponse = Response(
      Await.result(nettyResponseFuture))

    assert(
      expectedStatus == null || finagleResponse.status == expectedStatus,
      "Sent " + request + " and expected " + expectedStatus + " but received " + finagleResponse.status)

    assert(
      withBody == null || finagleResponse.contentString == withBody,
      "Sent " + request + " and expected body " + withBody + " but received \"" + finagleResponse.contentString + "\"")

    info("Sent " + request + " and received " + finagleResponse.status)
  }

  private def executeRequest(request: Request, routeToAdminServer: Boolean): Future[HttpResponse] = {
    request.headers().set("Host", "127.0.0.1") /* Mutation */

    if (request.uri.startsWith("/admin/finatra/"))
      router.services.adminService(request)
    else if (request.uri.startsWith("/admin") || routeToAdminServer)
      HttpMuxer(request)
    else
      router.services.externalService(request)
  }
}
