package com.twitter.finatra.sample

import com.google.inject.Module
import com.twitter.finatra.sample.modules.QueueModule
import com.twitter.inject.Logging
import com.twitter.inject.server.TwitterServer

object ExampleTwitterServerMain extends ExampleTwitterServer

class ExampleTwitterServer extends TwitterServer with Logging {

  override val modules: Seq[Module] = Seq(QueueModule)

  override protected def setup(): Unit = {
    // Create/start a pub-sub component and add it to the list of Awaitables, e.g., await(component)
    // It is important to remember to NOT BLOCK this method

    val publisher = injector.instance[Publisher]
    await(publisher)

    val subscriber = injector.instance[Subscriber]
    await(subscriber)

    closeOnExit(publisher)
    closeOnExitLast(subscriber)
  }

  override protected def start(): Unit = {
    val publisher = injector.instance[Publisher]
    val subscriber = injector.instance[Subscriber]

    subscriber.start()
    publisher.start()
  }
}
