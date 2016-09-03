package com.twitter.finatra.http.tests.integration.main;

import com.twitter.finatra.http.AbstractHttpServer;
import com.twitter.finatra.http.filters.CommonFilters;
import com.twitter.finatra.http.routing.HttpRouter;

public class DoEverythingJavaServer extends AbstractHttpServer {
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
