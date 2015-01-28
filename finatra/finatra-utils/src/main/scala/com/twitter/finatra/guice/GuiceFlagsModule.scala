package com.twitter.finatra.guice

import com.google.inject.{AbstractModule, Key}
import com.twitter.finatra.annotations.FlagImpl
import com.twitter.finatra.conversions.seq._
import com.twitter.finatra.utils.Logging

object GuiceFlagsModule {
  def create(flags: Seq[com.twitter.app.Flag[_]]) = {
    new GuiceFlagsModule(
      flags.createMap(
        keys = _.name,
        values = _.apply()))
  }
}

//TODO: Use type information in Flag instead of hardcoding java.lang.String
class GuiceFlagsModule(
  flags: Map[String, Any])
  extends AbstractModule
  with Logging {

  def configure() {
    for ((flagName, value) <- flags) {
      debug("Binding flag: " + flagName + " = " + value)
      val key = Key.get(classOf[java.lang.String], new FlagImpl(flagName))
      binder.bind(key).toInstance(value.toString)
    }
  }
}
