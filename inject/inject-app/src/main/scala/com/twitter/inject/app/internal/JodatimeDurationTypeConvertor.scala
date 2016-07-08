package com.twitter.inject.app.internal

import com.google.inject.TypeLiteral
import com.google.inject.spi.TypeConverter
import com.twitter.util.{Duration => TwitterDuration}
import org.joda.time.Duration

private[app] object JodatimeDurationTypeConvertor extends TypeConverter {
  def convert(value: String, toType: TypeLiteral[_]): Duration = {
    val twitterDuration = TwitterDuration.parse(value)
    Duration.millis(twitterDuration.inMillis)
  }
}
