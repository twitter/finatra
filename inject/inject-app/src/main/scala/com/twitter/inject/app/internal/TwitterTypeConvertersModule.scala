package com.twitter.inject.app.internal

import com.twitter.inject.TwitterModule
import com.twitter.inject.app.internal.converters.{
  InetSocketAddressTypeConverter,
  LocalTimeTypeConverter,
  StorageUnitTypeConverter,
  TwitterDurationTypeConverter,
  TwitterTimeTypeConverter
}
import com.twitter.util.{Duration, StorageUnit, Time}
import java.net.InetSocketAddress
import java.time.LocalTime
import org.joda.{time => joda}

private[app] object TwitterTypeConvertersModule extends TwitterModule {

  override def configure(): Unit = {
    addTypeConverter[joda.Duration](JodatimeDurationTypeConverter)
    addTypeConverter[LocalTime](LocalTimeTypeConverter)
    addTypeConverter[Time](TwitterTimeTypeConverter)
    addTypeConverter[Duration](TwitterDurationTypeConverter)
    addTypeConverter[StorageUnit](StorageUnitTypeConverter)
    addTypeConverter[InetSocketAddress](InetSocketAddressTypeConverter)
  }
}
