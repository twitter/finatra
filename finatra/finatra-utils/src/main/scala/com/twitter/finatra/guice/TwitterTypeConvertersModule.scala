package com.twitter.finatra.guice

import com.twitter.util.{Duration => TwitterDuration}
import org.joda.time.Duration

object TwitterTypeConvertersModule extends GuiceModule {

  override def configure() {
    addTypeConvertor[Duration](JodatimeDurationTypeConvertor)
    addTypeConvertor[TwitterDuration](TwitterDurationTypeConvertor)
  }
}
