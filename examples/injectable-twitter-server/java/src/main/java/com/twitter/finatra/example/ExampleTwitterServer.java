package com.twitter.finatra.example;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

import com.twitter.app.Flaggable;
import com.twitter.inject.server.AbstractTwitterServer;

public class ExampleTwitterServer extends AbstractTwitterServer {

  public ExampleTwitterServer() {
    createFlag(
        /* name = */ "subscriber.max.read",
        /* default = */ 5,
        /* help = */ "Subscriber Max read",
        /* flaggable = */ Flaggable.ofJavaInteger());
  }

  @Override
  public Collection<Module> javaModules() {
    return ImmutableList.of(QueueModule.getInstance());
  }

  @Override
  public void setup() {
    // Create/start a pub-sub component and add it to the list of Awaitables, e.g., await(component)
    // It is important to remember to NOT BLOCK this method. We add the publisher and subscriber to
    // the list of awaitables to block on as we want to ensure we entangle them together such that
    // if either exits, the entire server will exit.

    Publisher publisher = injector().instance(Publisher.class);
    await(publisher);

    Subscriber subscriber = injector().instance(Subscriber.class);
    await(subscriber);

    closeOnExit(publisher); // close publisher first to stop writing anything to the queue.
    closeOnExitLast(subscriber); // close subscriber last to ensure everything published is read.
  }

  @Override
  public void start() {
    Publisher publisher = injector().instance(Publisher.class);
    Subscriber subscriber = injector().instance(Subscriber.class);

    subscriber.start(); // start subscriber first in order to not miss anything published.

    // start publisher last to ensure we have a subscriber listening before publishing any item.
    publisher.start();
  }


  /**
   * Callback method run before {@code TwitterServer.postWarmup}, used for performing warm up of this server.
   * Override, but do not call {@code super.warmup()} as you will trigger a lint rule violation.
   *
   * Any exceptions thrown in this method will result in the app exiting.
   */
  @Override
  public void warmup() {
    // do nothing
  }
}
