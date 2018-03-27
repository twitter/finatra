package com.twitter.hello.server;

import javax.inject.Inject;

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
   * Define routes
   */
  public void configureRoutes() {
    get("/hello", request ->
        helloService.hi(request.getParam("name")));

    get("/goodbye", request ->
        new GoodbyeResponse("guest", "cya", 123));

    get("/ping", request ->
        Future.value("pong"));

    get("/exception", request ->
        new HelloWorldException("error processing request"));

    get("/magicNum", request ->
        Future.value(magicNumber));

    get("/moduleMagicNum", request ->
        Future.value(moduleMagicNumber));

    get("/moduleMagicFloatNum", request ->
        Future.value(moduleMagicFloatingNumber));
  }
}
