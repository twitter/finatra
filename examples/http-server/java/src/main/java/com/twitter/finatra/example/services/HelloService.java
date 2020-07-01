package com.twitter.finatra.example.services;

import javax.inject.Inject;

import com.twitter.finagle.http.Method;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finatra.example.domain.Cat;
import com.twitter.finatra.example.domain.Dog;
import com.twitter.finatra.http.marshalling.MessageBodyManager;
import com.twitter.finatra.http.response.ResponseBuilder;

public class HelloService {
  private final ResponseBuilder response;
  private final MessageBodyManager messageBodyManager;

  @Inject
  public HelloService(
      ResponseBuilder response,
      MessageBodyManager messageBodyManager) {
    this.response = response;
    this.messageBodyManager = messageBodyManager;
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
   * turns a cat into a dog
   */
  public Response cat(Request request) {
    Cat cat = messageBodyManager.read(request, Cat.class);
    Dog dog = new Dog();
    dog.setName(cat.getName());
    dog.setColor(cat.getColor());

    return response.ok(dog);
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
