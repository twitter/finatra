package com.twitter.hello.server;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

import com.twitter.finatra.http.JavaHttpServer;
import com.twitter.finatra.http.filters.CommonFilters;
import com.twitter.finatra.http.routing.HttpRouter;
import com.twitter.finatra.logging.modules.Slf4jBridgeModule$;

public class HelloWorldServer extends JavaHttpServer {

    @Override
    public Collection<Module> javaModules() {
        return ImmutableList.of(
                Slf4jBridgeModule$.MODULE$);
    }

    @Override
    public void configureHttp(HttpRouter httpRouter) {
        httpRouter
                .filter(CommonFilters.class)
                .add(HelloWorldController.class);
    }
}
