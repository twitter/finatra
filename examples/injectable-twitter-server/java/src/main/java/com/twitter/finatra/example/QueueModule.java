package com.twitter.finatra.example;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;
import com.twitter.inject.annotations.Flag;

/**
 * [[http://twitter.github.io/finatra/user-guide/getting-started/modules.html Modules]] are generally
 * useful for defining how to configure and instantiate classes which are not your own, e.g. a 3rd
 * party library's `DatabaseConnection` class.
 *
 * In this case, we're simply creating a Module that gives us a configured [[Queue]], which is
 * actually not necessary as we could simply annotate the [[Queue]] constructor and have the injector
 * provide a "just-in-time" [[https://github.com/google/guice/wiki/JustInTimeBindings binding]].
 *
 * For more information on modules see the link below.
 * @see [[http://go/docbird//finatra/user-guide/getting-started/modules.html#defining-modules Defining Modules]].
 */
public final class QueueModule extends TwitterModule {

  private static final QueueModule MODULE = new QueueModule();
  public static QueueModule getInstance() {
    return MODULE;
  }

  private QueueModule() {
    createFlag(
        "max.queue.size",
        6144,
        "Maximum queue size",
        Flaggable.ofJavaInteger());
  }


  @Provides
  @Singleton
  public Queue provideQueue(@Flag("max.queue.size") int maxQueueSize) {
    return new Queue(maxQueueSize);
  }

}
