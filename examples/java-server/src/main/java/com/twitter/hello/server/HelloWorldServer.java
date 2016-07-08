package com.twitter.hello.server;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

import com.twitter.finatra.logging.modules.Slf4jBridgeModule$;
import com.twitter.hello.HelloService;
import com.twitter.inject.server.AbstractTwitterServer;

public class HelloWorldServer extends AbstractTwitterServer {

    @Override
    public Collection<Module> javaModules() {
        return ImmutableList.<Module>of(
                Slf4jBridgeModule$.MODULE$);
    }

    @Override
    public void start() {
        HelloService helloService = injector().instance(HelloService.class);
        System.out.println(helloService.hi("Bob"));
    }
}
