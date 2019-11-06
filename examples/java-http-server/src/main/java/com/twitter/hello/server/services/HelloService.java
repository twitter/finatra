package com.twitter.hello.server.services;

import javax.inject.Inject;

import com.twitter.finagle.http.Method;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finatra.http.response.ResponseBuilder;

public class HelloService {
  private final ResponseBuilder response;

  @Inject
  public HelloService(ResponseBuilder response) {
    this.response = response;
  }

  /**
   * returns a String
   */
  public String hi(Request request) {
    return "Hello " + request.getParam("name");
  }

  /**
   * returns a GoodbyeResponse
   */
  public GoodbyeResponse goodbye(Request request) {
    return new GoodbyeResponse("guest", "cya", 123);
  }

  /**
   * returns an http Response
   */
  public Response echo(Request request) {
    return response.ok(request.getParam("q"));
  }

  /**
   * put, trace, & head should not return response bodies
   */
  public Response handleAnyMethod(Request request) {
    if (request.method().equals(Method.Get())
        || request.method().equals(Method.Post())
        || request.method().equals(Method.Patch())
        || request.method().equals(Method.Delete())
        || request.method().equals(Method.Connect())
        || request.method().equals(Method.Options())) {
      return response.ok(request.method().toString());
    } else {
      return response.ok();
    }
  }
}
