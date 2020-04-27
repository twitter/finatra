package com.twitter.finatra.example;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.inject.Module;

import com.twitter.app.Flaggable;
import com.twitter.inject.annotations.Flags;
import com.twitter.inject.app.AbstractApp;
import com.twitter.inject.modules.LoggerModule;
import com.twitter.inject.modules.StatsReceiverModule;
import com.twitter.util.logging.Logger;
import com.twitter.util.logging.Loggers;

public class HelloWorldApp extends AbstractApp {
  private static final Logger LOG = Loggers.getLogger("HelloWorldApp");

  private List<Integer> queue;

  public HelloWorldApp(List<Integer> q) {
    this.queue = q;

    flag().createMandatory(
        "username",
        "Username to use.",
        "-username=Bob",
        Flaggable.ofString()
    );
  }

  @Override
  public Collection<Module> javaModules() {
    return Collections.unmodifiableList(
        Arrays.asList(
          LoggerModule.apply(),
          StatsReceiverModule.apply()));
  }

  @Override
  public void run() {
    queue.add(3);
    HelloService helloService = injector().instance(HelloService.class);

    // username Flag is mandatory. if it has no value, the app fails here.
    String username = injector().instance(String.class, Flags.named("username"));
    LOG.debug(String.format("Input username: %s", username));
    LOG.info(helloService.hi(username));
  }

  @Override
  public void onInit() {
    queue.add(1);
  }

  @Override
  public void preMain() {
    queue.add(2);
  }

  @Override
  public void postMain() {
    queue.add(4);
  }

  @Override
  public void onExit() {
    queue.add(5);
  }

  @Override
  public void onExitLast() {
    queue.add(6);
  }

  public List<Integer> getQueue() {
    return queue;
  }
}
