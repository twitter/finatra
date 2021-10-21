package com.twitter.finatra.http.tests.integration.doeverything.main;

import java.util.Collection;
import java.util.Collections;

import scala.reflect.ManifestFactory;

import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finatra.http.AbstractHttpServer;
import com.twitter.finatra.http.filters.AccessLoggingFilter;
import com.twitter.finatra.http.filters.ExceptionMappingFilter;
import com.twitter.finatra.http.filters.HttpNackFilter;
import com.twitter.finatra.http.filters.HttpResponseFilter;
import com.twitter.finatra.http.filters.LoggingMDCFilter;
import com.twitter.finatra.http.filters.StatsFilter;
import com.twitter.finatra.http.filters.TraceIdMDCFilter;
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
        .filter(new TypeLiteral<LoggingMDCFilter<Request, Response>>(){})
        .filter(ManifestFactory.classType(TraceIdMDCFilter.class))
        .filter(new TypeLiteral<StatsFilter<Request>>() {})
        .filter(new TypeLiteral<AccessLoggingFilter<Request>>() {})
        .filter(new TypeLiteral<HttpResponseFilter<Request>>() {})
        .filter(new TypeLiteral<ExceptionMappingFilter<Request>>() {})
        .filter(new TypeLiteral<HttpNackFilter<Request>>() {})
        .filter(new AppendToHeaderJavaFilter("test", "1"))
        .add(DoEverythingJavaController.class)
        .add(new DoEverythingJavaNonInjectedController())
        .add(new AppendToHeaderJavaFilter("test", "2"),
            new DoEverythingJavaReadHeadersController())
        .add(AppendToHeaderJavaFilter.class, DoEverythingJavaReadHeadersInjectedController.class)
        .exceptionMapper(DoEverythingJavaExceptionMapper.class);
  }
}
