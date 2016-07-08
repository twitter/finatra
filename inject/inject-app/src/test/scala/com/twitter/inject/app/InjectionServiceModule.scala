package com.twitter.inject.app

import com.twitter.inject.TwitterModule

class InjectionServiceModule[T : Manifest](
  instance: T)
  extends TwitterModule {

  override def configure() = {
    bind[T].toInstance(instance)
  }
}
