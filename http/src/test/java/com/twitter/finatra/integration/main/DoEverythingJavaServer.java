package com.twitter.finatra.integration.main;

import com.twitter.finatra.http.JavaHttpServer;
import com.twitter.finatra.http.filters.CommonFilters;
import com.twitter.finatra.http.routing.HttpRouter;

public class DoEverythingJavaServer extends JavaHttpServer {
    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void configureHttp(HttpRouter httpRouter) {
        httpRouter
                .filter(CommonFilters.class)
                .add(DoEverythingJavaController.class);
    }
}
