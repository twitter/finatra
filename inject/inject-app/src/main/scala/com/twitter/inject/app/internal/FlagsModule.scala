package com.twitter.inject.app.internal

import com.google.inject.Key
import com.twitter.inject.annotations.Flags
import com.twitter.inject.{Logging, TwitterModule}
import javax.inject.Provider

private[app] object FlagsModule {
  def create(flags: Seq[com.twitter.app.Flag[_]]): FlagsModule = {
    val flagsMap = (for (flag <- flags) yield {
      flag.name -> flag.getWithDefault
    }).toMap

    new FlagsModule(flagsMap)
  }
}

private[app] class FlagsModule(flagsMap: Map[String, Option[Any]])
    extends TwitterModule
    with Logging {

  /**
   * Currently, we are unable to bind to anything other than a String type as Flags do not carry
   * enough type information due to type erasure. We would need to update [[com.twitter.app.Flag]]
   * or [[com.twitter.app.Flaggable]] to carry Manifest or TypeTag information which we could use
   * here to determine the correct type for building an instance Key. Or re-create the
   * [[com.twitter.app.Flaggable]] logic in a registered TypeConverter.
   *
   * In the interim, we use a String type and Guice + the Finatra framework provide a limited set of
   * conversions to other types, thus you can inject an @Flag of some non String types. But this is
   * not the same set supported by [[com.twitter.app.Flaggable]] and [[com.twitter.app.Flag]] as it
   * includes primarily String to other primitives, e.g, Byte, Short, Int, Long, Boolean, Double,
   * Float, Char, Enum and any other registered TypeConverters.
   *
   * @see [[TwitterTypeConvertersModule]]
   * @see [[https://github.com/google/guice/blob/master/core/src/com/google/inject/internal/TypeConverterBindingProcessor.java Guice Default Type Conversions]]
   */
  override def configure(): Unit = {
    for ((flagName, valueOpt) <- flagsMap) {
      val key: Key[String] = Flags.key(flagName)
      valueOpt match {
        case Some(value) =>
          debug("Binding flag: " + flagName + " = " + value)
          binder.bind(key).toInstance(value.toString)
        case None =>
          binder
            .bind(key)
            .toProvider(new Provider[Nothing] {
              override def get() =
                throw new IllegalArgumentException(
                  "flag without default: " + flagName + " has an unspecified value and is not eligible for @Flag injection"
                )
            })
      }
    }
  }
}
