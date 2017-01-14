package com.twitter.hello.server;

import javax.inject.Inject;

import com.twitter.finatra.http.AbstractController;
import com.twitter.util.Future;

public class HelloWorldController extends AbstractController {

    @Inject
    private HelloService helloService;

    /** Define routes */
    public void configureRoutes() {
        get("/hello", request ->
            helloService.hi(request.getParam("name")));

        get("/goodbye", request ->
            new GoodbyeResponse("guest", "cya", 123));

        get("/ping", request ->
            Future.value("pong"));

        get("/exception", request ->
            new HelloWorldException("error processing request"));
    }
}
