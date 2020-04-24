package com.twitter.finatra.http.tests.integration.doeverything.main;

import java.util.Collection;
import java.util.Collections;

import com.google.inject.Module;

import com.twitter.finatra.http.AbstractHttpServer;
import com.twitter.finatra.http.filters.CommonFilters;
import com.twitter.finatra.http.routing.HttpRouter;

public class DoEverythingJavaServer extends AbstractHttpServer {

  @Override
  public Collection<Module> javaModules() {
    return Collections.singletonList(new TestModuleB());
  }

  @Override
  public String name() {
    return this.getClass().getSimpleName();
  }

  @Override
  public void configureHttp(HttpRouter router) {
    router
        .filter(CommonFilters.class)
        .filter(new AppendToHeaderJavaFilter("test", "1"))
        .add(DoEverythingJavaController.class)
        .add(new DoEverythingJavaNonInjectedController())
        .add(new AppendToHeaderJavaFilter("test", "2"),
            new DoEverythingJavaReadHeadersController())
        .exceptionMapper(DoEverythingJavaExceptionMapper.class);
  }
}
