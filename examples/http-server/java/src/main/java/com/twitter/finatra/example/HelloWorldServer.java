package com.twitter.finatra.example;

import java.util.Collection;
import java.util.Collections;

import scala.reflect.ManifestFactory;

import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import com.twitter.app.Flaggable;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finatra.example.controllers.HelloWorldController;
import com.twitter.finatra.example.exceptions.HelloWorldExceptionMapper;
import com.twitter.finatra.example.filters.AppendToHeaderFilter;
import com.twitter.finatra.example.modules.MagicNumberModule;
import com.twitter.finatra.http.AbstractHttpServer;
import com.twitter.finatra.http.filters.AccessLoggingFilter;
import com.twitter.finatra.http.filters.ExceptionMappingFilter;
import com.twitter.finatra.http.filters.HttpNackFilter;
import com.twitter.finatra.http.filters.HttpResponseFilter;
import com.twitter.finatra.http.filters.LoggingMDCFilter;
import com.twitter.finatra.http.filters.StatsFilter;
import com.twitter.finatra.http.filters.TraceIdMDCFilter;
import com.twitter.finatra.http.routing.HttpRouter;

public class HelloWorldServer extends AbstractHttpServer {

  public HelloWorldServer() {
    createFlag(
        /* name = */ "magic.number",
        /* default = */ 55,
        /* help = */ "This is a magic number.",
        /* flaggable = */ Flaggable.ofJavaInteger());
  }

  @Override
  public Collection<Module> javaModules() {
    return Collections.singletonList(new MagicNumberModule());
  }

  @Override
  public void configureHttp(HttpRouter router) {
    router
        .filter(new TypeLiteral<LoggingMDCFilter<Request, Response>>(){})
        // note this binds the TraceIdMDCFilter type without the parameterized types which in
        // this case is ok since the filter is appended to a fully typed
        // filter (the loggingMDCFilter).
        .filter(ManifestFactory.classType(TraceIdMDCFilter.class))
        .filter(new TypeLiteral<StatsFilter<Request>>() {})
        .filter(new TypeLiteral<AccessLoggingFilter<Request>>() {})
        .filter(new TypeLiteral<HttpResponseFilter<Request>>() {})
        .filter(new TypeLiteral<ExceptionMappingFilter<Request>>() {})
        .filter(new TypeLiteral<HttpNackFilter<Request>>() {})
        .filter(new AppendToHeaderFilter("foo", "1"))
        .add(HelloWorldController.class)
        .register(ManifestFactory.classType(CatMessageBodyReader.class))
        .register(ManifestFactory.classType(DogMessageBodyWriter.class))
        .exceptionMapper(HelloWorldExceptionMapper.class);
  }
}
