package com.twitter.finatra.example;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.twitter.app.Flaggable;
import com.twitter.inject.TwitterModule;
import com.twitter.inject.annotations.Flag;

/**
 * <a href="https://twitter.github.io/finatra/user-guide/getting-started/modules.html">Modules</a> are
 * generally useful for defining how to configure and instantiate classes which are not your own,
 * e.g. a 3rd party library's `DatabaseConnection` class.
 *
 * In this case, we're simply creating a Module that gives us a configured [[Queue]], which is
 * actually not necessary as we could simply annotate the [[Queue]] constructor and have the injector
 * provide a "just-in-time" <a href="https://github.com/google/guice/wiki/JustInTimeBindings">binding</a>.
 *
 * For more information on modules see the link below.
 * @see <a href="https://twitter.github.io/finatra/user-guide/getting-started/modules.html#defining-modules>Defining Modules</a>
 */
public final class QueueModule extends TwitterModule {

  private static final QueueModule MODULE = new QueueModule();
  public static QueueModule getInstance() {
    return MODULE;
  }

  private QueueModule() {
    createFlag(
        /* name = */ "max.queue.size",
        /* default = */ 6144,
        /* help = */ "Maximum queue size",
        /* flaggable = */ Flaggable.ofJavaInteger());
  }


  @Provides
  @Singleton
  public Queue provideQueue(@Flag("max.queue.size") int maxQueueSize) {
    return new Queue(maxQueueSize);
  }
}
