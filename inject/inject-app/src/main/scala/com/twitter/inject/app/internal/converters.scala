package com.twitter.inject.app.internal

import com.google.inject.TypeLiteral
import com.google.inject.spi.TypeConverter
import com.twitter.app.Flaggable
import com.twitter.util.{Duration, StorageUnit, Time}
import java.net.InetSocketAddress
import java.time.LocalTime

private[app] object converters {

  object LocalTimeTypeConverter extends TypeConverter {
    def convert(value: String, toType: TypeLiteral[_]): LocalTime =
      LocalTime.parse(value)
  }

  object TwitterDurationTypeConverter extends TypeConverter {
    def convert(value: String, toType: TypeLiteral[_]): Duration =
      Duration.parse(value)
  }

  /** @see [[com.twitter.app.Flaggable.ofTime]] */
  object TwitterTimeTypeConverter extends TypeConverter {
    def convert(value: String, toType: TypeLiteral[_]): Time =
      Flaggable.ofTime.parse(value)
  }

  object StorageUnitTypeConverter extends TypeConverter {
    def convert(value: String, toType: TypeLiteral[_]): StorageUnit =
      StorageUnit.parse(value)
  }

  /** @see [[com.twitter.app.Flaggable.ofInetSocketAddress]] */
  object InetSocketAddressTypeConverter extends TypeConverter {
    def convert(value: String, toType: TypeLiteral[_]): InetSocketAddress =
      Flaggable.ofInetSocketAddress.parse(value)
  }
}
