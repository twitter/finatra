package com.twitter.hello.server.exceptions;

import javax.inject.Inject;

import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finatra.http.exceptions.AbstractExceptionMapper;
import com.twitter.finatra.http.response.ResponseBuilder;

public class HelloWorldExceptionMapper
    extends AbstractExceptionMapper<HelloWorldException> {

  private ResponseBuilder response;

  @Inject
  public HelloWorldExceptionMapper(ResponseBuilder responseBuilder) {
    this.response = responseBuilder;
  }

  @Override
  public Response toResponse(Request request, HelloWorldException throwable) {
    return response.internalServerError(throwable.getMessage());
  }
}
