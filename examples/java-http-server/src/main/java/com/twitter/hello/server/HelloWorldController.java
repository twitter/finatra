package com.twitter.hello.server;

import javax.inject.Inject;

import com.twitter.finatra.http.JavaController;
import com.twitter.util.Future;

public class HelloWorldController extends JavaController {

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
    }
}
