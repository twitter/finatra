package com.twitter.hello.server;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.twitter.hello.HelloService;
import com.twitter.inject.server.AbstractTwitterServer;

public class HelloWorldServer extends AbstractTwitterServer {
  private ConcurrentLinkedQueue<Integer> queue;

  public HelloWorldServer(ConcurrentLinkedQueue<Integer> q) {
    this.queue = q;
  }

  @Override
  public void start() {
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
    queue.add(6);
  }

  @Override
  public void onExit() {
    queue.add(4);
  }

  @Override
  public void onExitLast() {
    queue.add(5);
  }

  public ConcurrentLinkedQueue<Integer> getQueue() {
    return queue;
  }
}
