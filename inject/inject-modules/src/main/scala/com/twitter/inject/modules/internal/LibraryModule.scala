package com.twitter.inject.modules.internal

import com.twitter.inject.TwitterModule
import com.twitter.inject.internal.Library

/* exposed for testing */
private[twitter] class LibraryModule(name: String) extends TwitterModule {
  override protected def configure(): Unit = bind[Library].toInstance(Library(name))
}
