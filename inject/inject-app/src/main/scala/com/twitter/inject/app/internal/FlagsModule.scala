package com.twitter.inject.app.internal

import com.google.inject.Key
import com.twitter.finatra.annotations.FlagImpl
import com.twitter.inject.{TwitterModule, Logging}

object FlagsModule {
  def create(flags: Seq[com.twitter.app.Flag[_]]) = {
    val flagsMap = (for (flag <- flags) yield {
      flag.name -> flag.getWithDefault
    }).toMap

    new FlagsModule(flagsMap)
  }
}

//TODO: Use type information in Flag instead of hardcoding java.lang.String
class FlagsModule(
  flagsMap: Map[String, Option[Any]])
  extends TwitterModule
  with Logging {

  override def configure() {
    for ((flagName, valueOpt) <- flagsMap) {
      valueOpt match {
        case Some(value) =>
          debug("Binding flag: " + flagName + " = " + value)
          val key = Key.get(classOf[java.lang.String], new FlagImpl(flagName))
          binder.bind(key).toInstance(value.toString)
        case None =>
          warn("flag without default: " + flagName + " has an unspecified value and is not eligible for @Flag injection")
      }
    }
  }
}
