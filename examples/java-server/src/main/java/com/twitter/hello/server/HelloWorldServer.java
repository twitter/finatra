package com.twitter.hello.server;

import com.twitter.hello.HelloService;
import com.twitter.inject.server.AbstractTwitterServer;

public class HelloWorldServer extends AbstractTwitterServer {

    @Override
    public void start() {
        HelloService helloService = injector().instance(HelloService.class);
        System.out.println(helloService.hi("Bob"));
    }
}
