package com.twitter.inject.app.internal

import com.google.inject.TypeLiteral
import com.google.inject.spi.TypeConverter
import com.twitter.util.{Duration => TwitterDuration}
import org.joda.time.Duration

@deprecated("Users are encouraged to use the standard JDK java.time.LocalTime", "2020-04-21")
private[app] object JodatimeDurationTypeConverter extends TypeConverter {
  def convert(value: String, toType: TypeLiteral[_]): Duration = {
    val twitterDuration = TwitterDuration.parse(value)
    Duration.millis(twitterDuration.inMillis)
  }
}
