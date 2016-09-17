package com.twitter.hello.app;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

import com.twitter.hello.HelloService;
import com.twitter.inject.app.AbstractApp;
import com.twitter.inject.logging.modules.LoggerModule$;

public class HelloWorldApp extends AbstractApp {

    @Override
    public Collection<Module> javaModules() {
        return ImmutableList.<Module>of(
                LoggerModule$.MODULE$);
    }

    @Override
    public void run() {
        HelloService helloService = injector().instance(HelloService.class);
        System.out.println(helloService.hi("Bob"));
    }
}
