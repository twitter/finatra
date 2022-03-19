package com.twitter.inject.app.internal

import com.twitter.inject.TwitterModule
import com.twitter.inject.app.console.ConsoleWriter

object ConsoleWriterModule extends TwitterModule {
  override def configure(): Unit =
    bind[ConsoleWriter].toInstance(new ConsoleWriter(Console.out, Console.err))
}
