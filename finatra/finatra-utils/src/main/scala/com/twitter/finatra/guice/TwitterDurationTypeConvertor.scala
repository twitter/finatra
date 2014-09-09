package com.twitter.finatra.guice

import com.google.inject.TypeLiteral
import com.google.inject.spi.TypeConverter
import com.twitter.util.{Duration => TwitterDuration}

object TwitterDurationTypeConvertor extends TypeConverter {
  def convert(value: String, toType: TypeLiteral[_]): TwitterDuration = {
    TwitterDuration.parse(value)
  }
}
