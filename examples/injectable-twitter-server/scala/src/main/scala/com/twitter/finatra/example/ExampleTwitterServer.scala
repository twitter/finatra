package com.twitter.finatra.example

import com.google.inject.Module
import com.twitter.inject.Logging
import com.twitter.inject.server.TwitterServer

object ExampleTwitterServerMain extends ExampleTwitterServer

class ExampleTwitterServer extends TwitterServer with Logging {
  flag[Int]("subscriber.max.read", 5, "Subscriber Max read")

  override val modules: Seq[Module] = Seq(QueueModule)

  override protected def setup(): Unit = {
    // Create/start a pub-sub component and add it to the list of Awaitables, e.g., await(component)
    // It is important to remember to NOT BLOCK this method. We add the publisher and subscriber to
    // the list of awaitables to block on as we want to ensure we entangle them together such that
    // if either exits, the entire server will exit.

    val publisher = injector.instance[Publisher]
    await(publisher)

    val subscriber = injector.instance[Subscriber]
    await(subscriber)

    closeOnExit(publisher) // close publisher first to stop writing anything to the queue
    closeOnExitLast(subscriber) // close subscriber last to ensure everything published is read.
  }

  override protected def start(): Unit = {
    val publisher = injector.instance[Publisher]
    val subscriber = injector.instance[Subscriber]

    subscriber.start() // start subscriber first in order to not miss anything published.

    // start publisher last to ensure we have a subscriber listening before publishing any item.
    publisher.start()
  }

  /**
   * Callback method run before [[TwitterServer.postWarmup]], used for performing warm up of this server.
   * Override, but do not call `super.warmup()` as you will trigger a lint rule violation.
   *
   * Any exceptions thrown in this method will result in the app exiting.
   */
  override protected def warmup(): Unit = { /* do nothing */ }
}
