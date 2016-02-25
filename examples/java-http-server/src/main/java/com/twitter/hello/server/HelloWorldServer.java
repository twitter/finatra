package com.twitter.hello.server;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

import com.twitter.finatra.http.JavaHttpServer;
import com.twitter.finatra.http.filters.CommonFilters;
import com.twitter.finatra.http.routing.HttpRouter;

public class HelloWorldServer extends JavaHttpServer {

    @Override
    public void configureHttp(HttpRouter httpRouter) {
        httpRouter
                .filter(CommonFilters.class)
                .add(HelloWorldController.class);
    }
}
