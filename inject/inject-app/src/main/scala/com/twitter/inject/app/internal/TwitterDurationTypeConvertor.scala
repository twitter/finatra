package com.twitter.inject.app.internal

import com.google.inject.TypeLiteral
import com.google.inject.spi.TypeConverter
import com.twitter.util.{Duration => TwitterDuration}

private[app] object TwitterDurationTypeConvertor extends TypeConverter {
  def convert(value: String, toType: TypeLiteral[_]): TwitterDuration = {
    TwitterDuration.parse(value)
  }
}
