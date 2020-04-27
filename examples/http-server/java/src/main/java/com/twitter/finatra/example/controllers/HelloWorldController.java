package com.twitter.finatra.example.controllers;

import javax.inject.Inject;

import com.twitter.finagle.http.Request;
import com.twitter.finatra.example.exceptions.HelloWorldException;
import com.twitter.finatra.example.filters.AppendToHeaderFilter;
import com.twitter.finatra.example.services.HelloService;
import com.twitter.finatra.http.AbstractController;
import com.twitter.inject.annotations.Flag;
import com.twitter.util.Future;

public class HelloWorldController extends AbstractController {

  private final HelloService helloService;

  private final Integer magicNumber;

  private final Integer moduleMagicNumber;

  private final Float moduleMagicFloatingNumber;

  @Inject
  public HelloWorldController(
      HelloService helloService,
      @Flag("magic.number") Integer magicNumber,
      @Flag("module.magic.number") Integer moduleMagicNumber,
      @Flag("module.magic.float") Float moduleMagicFloatingNumber) {
    this.helloService = helloService;
    this.magicNumber = magicNumber;
    this.moduleMagicNumber = moduleMagicNumber;
    this.moduleMagicFloatingNumber = moduleMagicFloatingNumber;
  }

  /**
   * Define routes. Note that typing the lambda input is optional here because
   * the callback type is already specified to be the concrete type of [[com.twitter.finagle.http.Request]].
   * We only include it here as an example of how to construct a lambda input with a type.
   */
  public void configureRoutes() {
    Future<Integer> magicNumberF = Future.value(magicNumber);

    get("/hello", helloService::hi);

    get("/goodbye", helloService::goodbye);

    get("/echo", helloService::echo);

    filter(new AppendToHeaderFilter("foo", "2"))
        .filter(new AppendToHeaderFilter("foo", "3"))
        .get("/ping", request -> {
              assert request.headerMap().getAll("foo").mkString("").equals("123");
              return Future.value("pong");
            }
        );

    get("/exception", request -> new HelloWorldException("error processing request"));

    get("/magicNum", (Request request) -> magicNumberF);

    prefix("/v2").get("/magicNum", request -> magicNumberF);

    get("/moduleMagicNum", (Request request) -> Future.value(moduleMagicNumber));

    get("/moduleMagicFloatNum", request -> Future.value(moduleMagicFloatingNumber));

    post("/catFancy", helloService::cat);

    any("/any/method", helloService::handleAnyMethod);
  }
}
