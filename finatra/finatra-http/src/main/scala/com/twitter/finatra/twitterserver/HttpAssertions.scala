package com.twitter.finatra.twitterserver

import com.twitter.finagle.http._
import com.twitter.finatra.twitterserver.routing.{NotFoundService, Router}
import com.twitter.finatra.utils.Logging
import com.twitter.util.{Await, Future}
import javax.inject.Inject
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpResponseStatus}

class HttpAssertions @Inject()(
  router: Router)
  extends Logging {

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

  /* Private */

  private def send(request: Request, expectedStatus: HttpResponseStatus, expectedBody: String, routeToAdminServer: Boolean) {
    val httpResponse = Await.result(executeRequest(request, routeToAdminServer))
    val response = Response(httpResponse)

    assert(
      expectedStatus == null || response.status == expectedStatus,
      "Sent " + request + " and expected " + expectedStatus + " but received " + response.status)

    assert(
      expectedBody == null || response.contentString == expectedBody,
      "Sent " + request + " and expected body " + expectedBody + " but received \"" + response.contentString + "\"")

    info("Sent " + request + " and received " + response.status)
  }

  // Check Finatra's Admin RoutingController before checking the global HttpMuxer
  // We have to check both, since we don't add our Admin RoutingController to HttpMuxer until after warmup
  private def executeRequest(request: Request, routeToAdminServer: Boolean): Future[HttpResponse] = {
    request.headers().set("Host", "127.0.0.1") /* Mutation */

    if (request.uri.startsWith("/admin") || routeToAdminServer) {
      router.services.adminService(request) flatMap { response =>
        if (isAdminRoutingControllerNotFound(request, response))
          HttpMuxer(request)
        else
          Future(response)
      }
    }
    else {
      router.services.externalService(request)
    }
  }

  private def isAdminRoutingControllerNotFound(request: Request, response: Response): Boolean = {
    response.status == Status.NotFound &&
      response.contentString == NotFoundService.notFound(request.uri).contentString
  }
}
