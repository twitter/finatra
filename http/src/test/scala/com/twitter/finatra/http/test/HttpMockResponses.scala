package com.twitter.finatra.http.test

import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import com.twitter.finatra.http.internal.marshalling.mustache.MustacheService
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.http.routing.FileResolver
import com.twitter.finatra.json.FinatraObjectMapper
import org.jboss.netty.handler.codec.http.HttpResponseStatus

trait HttpMockResponses {

  //NOTE: TestResponses may not be able to use all features provided by normally injected dependencies
  protected lazy val testResponseBuilder = new ResponseBuilder(
    FinatraObjectMapper.create(),
    new FileResolver("src/main/webapp/"),
    new MessageBodyManager(null, null, null),
    new MustacheService(null))

  def ok = testResponseBuilder.ok

  def ok(body: Any) = testResponseBuilder.ok.body(body)

  def created = testResponseBuilder.created

  def accepted = testResponseBuilder.accepted

  def forbidden = testResponseBuilder.forbidden

  def notFound = testResponseBuilder.notFound

  def internalServerError = testResponseBuilder.internalServerError

  def internalServerError(body: Any) = testResponseBuilder.internalServerError.body(body)

  def clientClosed = testResponseBuilder.clientClosed

  def response(statusCode: Int) = testResponseBuilder.status(statusCode)

  def response(httpResponseStatus: HttpResponseStatus) = testResponseBuilder.status(httpResponseStatus)
}
