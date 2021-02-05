package com.twitter.finatra.example

import com.google.inject.Module
import com.twitter.inject.annotations.Flags
import com.twitter.inject.app.App
import com.twitter.inject.modules.{LoggerModule, StatsReceiverModule}
import com.twitter.util.logging.Logger
import scala.collection.mutable.ArrayBuffer

object SampleAppMain extends SampleApp

class SampleApp extends App {
  private[this] val log: Logger = Logger("SampleApp")

  private[this] val queue: ArrayBuffer[Int] = new ArrayBuffer[Int]()

  flag[String]("username", "Username to use.", "-username=Bob")

  override val modules: Seq[Module] = Seq(
    LoggerModule,
    StatsReceiverModule
  )

  override protected def run(): Unit = {
    queue += 3
    val helloService: HelloService = injector.instance[HelloService]

    // username Flag is mandatory. if it has no value, the app fails here.
    val username: String = injector.instance[String](Flags.named("username"))
    log.debug(s"Input username: $username")
    log.info(helloService.hi(username))
  }

  init {
    queue += 1
  }

  premain {
    queue += 2
  }

  postmain {
    queue += 4
  }

  onExit {
    queue += 5
  }

  onExitLast {
    queue += 6
  }

  def getQueue: Seq[Int] = this.queue.toSeq
}
