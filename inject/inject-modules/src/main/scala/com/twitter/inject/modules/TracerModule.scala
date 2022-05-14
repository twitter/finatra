package com.twitter.inject.modules

import com.twitter.finagle.tracing.Tracer
import com.twitter.inject.TwitterModule

object TracerModule extends TwitterModule {
  override def configure(): Unit = {
    bindOption[Tracer]
  }

  /**  Java-friendly way to access this module as a singleton instance */
  def get(): this.type = this
}
