package com.twitter.hello.app;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.inject.Module;

import com.twitter.hello.HelloService;
import com.twitter.inject.app.AbstractApp;
import com.twitter.inject.modules.LoggerModule$;

public class HelloWorldApp extends AbstractApp {
  private List<Integer> queue;

  public HelloWorldApp(List<Integer> q) {
    this.queue = q;
  }

  @Override
  public Collection<Module> javaModules() {
    return Collections.singletonList(
        LoggerModule$.MODULE$);
  }

  @Override
  public void run() {
    queue.add(3);
    HelloService helloService = injector().instance(HelloService.class);
    System.out.println(helloService.hi("Bob"));
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
