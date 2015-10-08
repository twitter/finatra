package com.twitter.finatra.utils

import com.twitter.finagle.http.{Response, Status}
import com.twitter.finagle.http.Status._

object ResponseUtils {

  def is2xxResponse(response: Response): Boolean = {
    errorClass(response) == 2
  }

  def is5xxResponse(response: Response) = {
    errorClass(response) == 5
  }

  def is4xxOr5xxResponse(response: Response) = {
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

  private def errorClass(response: Response): Int = {
    response.statusCode / 100
  }

  private def expectResponseStatus(
    response: Response)(
    expectedStatus: Status = null,
    withBody: String = null): Unit = {

    assert(
      expectedStatus == null || response.status == expectedStatus,
      "Expected " + expectedStatus + " but received " + response.status)

    assert(
      withBody == null || response.contentString == withBody,
      "Expected body " + withBody + " but received \"" + response.contentString + "\"")
  }
}
