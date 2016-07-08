package com.twitter.inject.app.internal

import com.twitter.inject.TwitterModule
import com.twitter.util.{Duration => TwitterDuration}
import org.joda.time.Duration

private[app] object TwitterTypeConvertersModule extends TwitterModule {

  override def configure() {
    addTypeConvertor[Duration](JodatimeDurationTypeConvertor)
    addTypeConvertor[TwitterDuration](TwitterDurationTypeConvertor)
  }
}
