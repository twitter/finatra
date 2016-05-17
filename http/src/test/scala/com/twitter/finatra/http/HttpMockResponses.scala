package com.twitter.finatra.http

import com.twitter.finagle.http.{Response, Status, Version}

trait HttpMockResponses {

  def ok: Response = response(Status.Ok)

  def ok(body: String): Response = {
    val resp = response(Status.Ok)
    resp.setContentString(body)
    resp
  }

  def created: Response = response(Status.Created)

  def accepted: Response = response(Status.Accepted)

  def forbidden: Response = response(Status.Forbidden)

  def notFound: Response = response(Status.NotFound)

  def internalServerError: Response = response(Status.InternalServerError)

  def internalServerError(body: String): Response = {
    val resp = response(Status.InternalServerError)
    resp.setContentString(body)
    resp
  }

  def clientClosed: Response = response(Status.ClientClosedRequest)

  def response(statusCode: Int): Response = response(Status(statusCode))

  def response(status: Status): Response = Response(Version.Http11, status)
}
