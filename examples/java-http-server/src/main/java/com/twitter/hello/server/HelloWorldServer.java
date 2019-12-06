package com.twitter.hello.server;

import java.util.Collection;

import scala.reflect.ManifestFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

import com.twitter.app.Flaggable;
import com.twitter.finatra.http.AbstractHttpServer;
import com.twitter.finatra.http.filters.CommonFilters;
import com.twitter.finatra.http.routing.HttpRouter;
import com.twitter.hello.server.controllers.HelloWorldController;
import com.twitter.hello.server.exceptions.HelloWorldExceptionMapper;
import com.twitter.hello.server.filters.AppendToHeaderFilter;
import com.twitter.hello.server.modules.MagicNumberModule;

public class HelloWorldServer extends AbstractHttpServer {

  public HelloWorldServer() {
    flag().create("magic.number", 55, "This is a magic number.", Flaggable.ofJavaInteger());
  }

  @Override
  public Collection<Module> javaModules() {
    return ImmutableList.<Module>of(
        new MagicNumberModule());
  }

  @Override
  public void configureHttp(HttpRouter httpRouter) {
    httpRouter
        .filter(CommonFilters.class)
        .filter(new AppendToHeaderFilter("foo", "1"))
        .add(HelloWorldController.class)
        .register(ManifestFactory.classType(CatMessageBodyReader.class))
        .register(ManifestFactory.classType(DogMessageBodyWriter.class))
        .exceptionMapper(HelloWorldExceptionMapper.class);
  }
}
