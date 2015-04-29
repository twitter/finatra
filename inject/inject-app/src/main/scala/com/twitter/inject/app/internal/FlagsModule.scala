package com.twitter.inject.app.internal

import com.google.inject.Key
import com.twitter.finatra.annotations.FlagImpl
import com.twitter.inject.{TwitterModule, Logging}

object FlagsModule {
  def create(flags: Seq[com.twitter.app.Flag[_]]) = {
    val flagsMap = (for (flag <- flags) yield {
      flag.name -> flag()
    }).toMap

    new FlagsModule(flagsMap)
  }
}

//TODO: Use type information in Flag instead of hardcoding java.lang.String
class FlagsModule(
  flags: Map[String, Any])
  extends TwitterModule
  with Logging {

  override def configure() {
    for ((flagName, value) <- flags) {
      debug("Binding flag: " + flagName + " = " + value)
      val key = Key.get(classOf[java.lang.String], new FlagImpl(flagName))
      binder.bind(key).toInstance(value.toString)
    }
  }
}
