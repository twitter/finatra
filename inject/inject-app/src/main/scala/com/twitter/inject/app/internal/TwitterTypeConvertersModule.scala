package com.twitter.inject.app.internal

import com.twitter.inject.TwitterModule
import org.joda.{time => joda}

private[app] object TwitterTypeConvertersModule extends TwitterModule {

  override def configure(): Unit = {
    // Single value type converters
    addTypeConverter[joda.Duration](JodatimeDurationTypeConverter)
  }
}
