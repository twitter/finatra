package com.twitter.inject.internal.modules

import com.twitter.inject.TwitterModule
import com.twitter.inject.internal.Library

private[twitter] class LibraryModule(name: String) extends TwitterModule {
  override protected def configure(): Unit = bindSingleton[Library].toInstance(Library(name))
}
