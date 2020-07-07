package com.twitter.finatra

import com.twitter.finagle.stats.DefaultStatsReceiver
import com.twitter.finagle.toggle.{StandardToggleMap, ToggleMap}

package object kafka {

  private[this] val LibraryName: String = "com.twitter.finatra.kafka"

  /**
   * The [[ToggleMap]] used for finatra-kafka
   */
  private[kafka] val Toggles: ToggleMap = StandardToggleMap(LibraryName, DefaultStatsReceiver)

  private[kafka] val TracingEnabledToggleId: String = "com.twitter.finatra.kafka.TracingEnabled"
}
