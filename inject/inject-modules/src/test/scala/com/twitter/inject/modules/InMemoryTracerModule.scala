package com.twitter.inject.modules

import com.twitter.finagle.tracing.Tracer
import com.twitter.inject.InMemoryTracer
import com.twitter.inject.TwitterModule

import javax.inject.Singleton

object InMemoryTracerModule extends TwitterModule {
  override def configure(): Unit = {
    bind[Tracer].to[InMemoryTracer].in[Singleton]
  }

  /**  Java-friendly way to access this module as a singleton instance */
  def get(): this.type = this
}
