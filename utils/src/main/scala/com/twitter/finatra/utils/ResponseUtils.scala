package com.twitter.finatra.utils

import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status._
import org.jboss.netty.handler.codec.http.{HttpResponseStatus, HttpResponse}

object ResponseUtils {

  def is5xxResponse(response: HttpResponse) = {
    errorClass(response) == 5
  }

  def is4xxOr5xxResponse(response: HttpResponse) = {
    val errClass = errorClass(response)
    errClass == 4 || errClass == 5
  }

  def expectOkResponse(response: Response): Unit = {
    expectResponseStatus(response)(Ok)
  }

  def expectOkResponse(response: Response, withBody: String = null): Unit = {
    expectResponseStatus(response)(Ok, withBody)
  }

  def expectForbiddenResponse(response: Response): Unit = {
    expectResponseStatus(response)(Forbidden)
  }

  def expectForbiddenResponse(response: Response, withBody: String = null): Unit = {
    expectResponseStatus(response)(Forbidden, withBody)
  }

  def expectNotFoundResponse(response: Response): Unit = {
    expectResponseStatus(response)(NotFound)
  }

  def expectNotFoundResponse(response: Response, withBody: String = null): Unit = {
    expectResponseStatus(response)(NotFound, withBody)
  }


  /* Private */

  private def errorClass(response: HttpResponse): Int = {
    response.getStatus.getCode / 100
  }

  private def expectResponseStatus(
    response: Response)(
    expectedStatus: HttpResponseStatus = null,
    withBody: String = null): Unit = {

    assert(
      expectedStatus == null || response.status == expectedStatus,
      "Expected " + expectedStatus + " but received " + response.status)

    assert(
      withBody == null || response.contentString == withBody,
      "Expected body " + withBody + " but received \"" + response.contentString + "\"")
  }
}
