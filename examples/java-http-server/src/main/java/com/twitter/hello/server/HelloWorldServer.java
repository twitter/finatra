package com.twitter.hello.server;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

import com.twitter.app.Flag;
import com.twitter.app.Flaggable;
import com.twitter.finatra.http.AbstractHttpServer;
import com.twitter.finatra.http.filters.CommonFilters;
import com.twitter.finatra.http.routing.HttpRouter;
import com.twitter.hello.server.modules.MagicNumberModule;

public class HelloWorldServer extends AbstractHttpServer {

    private final Flag<Integer> magicNumber = flag().create("magic.number", 55,
        "This is a magic number.", Flaggable.ofJavaInteger());

    @Override
    public Collection<Module> javaModules() {
        return ImmutableList.<Module>of(
            new MagicNumberModule());
    }

    @Override
    public void configureHttp(HttpRouter httpRouter) {
        httpRouter
            .filter(CommonFilters.class)
            .add(HelloWorldController.class)
            .exceptionMapper(HelloWorldExceptionMapper.class);
    }
}
