package com.twitter.inject.app.internal

import com.twitter.inject.TwitterModule
import com.twitter.util.{Duration, StorageUnit, Time}
import java.net.InetSocketAddress
import java.time.LocalTime
import org.joda.{time => joda}

private[app] object TwitterTypeConvertersModule extends TwitterModule {

  override def configure(): Unit = {
    // Single value type converters
    addTypeConverter[joda.Duration](JodatimeDurationTypeConverter)
    addFlagConverter[LocalTime]
    addFlagConverter[Time]
    addFlagConverter[Duration]
    addFlagConverter[StorageUnit]
    addFlagConverter[InetSocketAddress]

    // Multi-value type converters (comma-separated) for scala.Seq
    addFlagConverter[Seq[String]]
    addFlagConverter[Seq[Int]]
    addFlagConverter[Seq[Long]]
    addFlagConverter[Seq[Double]]
    addFlagConverter[Seq[Float]]
    addFlagConverter[Seq[LocalTime]]
    addFlagConverter[Seq[Time]]
    addFlagConverter[Seq[Duration]]
    addFlagConverter[Seq[StorageUnit]]
    addFlagConverter[Seq[InetSocketAddress]]

    // Multi-value type converters (comma-separated) for java.util.List
    addFlagConverter[java.util.List[String]]
    addFlagConverter[java.util.List[Int]]
    addFlagConverter[java.util.List[Long]]
    addFlagConverter[java.util.List[Double]]
    addFlagConverter[java.util.List[Float]]
    addFlagConverter[java.util.List[LocalTime]]
    addFlagConverter[java.util.List[Time]]
    addFlagConverter[java.util.List[Duration]]
    addFlagConverter[java.util.List[StorageUnit]]
    addFlagConverter[java.util.List[InetSocketAddress]]
  }
}
