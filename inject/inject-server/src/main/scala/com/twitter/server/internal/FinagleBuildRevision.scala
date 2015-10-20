package com.twitter.server.internal

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.{Injector, Logging}
import com.twitter.util.NonFatal
import com.twitter.util.U64._

object FinagleBuildRevision extends Logging {

  def register(injector: Injector): Unit = {
    com.twitter.finagle.Init.finagleBuildRevision match {
      case "?" =>
        warn("Unable to resolve Finagle revision.")
      case revision =>
        info(s"Resolved Finagle build revision: (rev=$revision)")
        injector.instance[StatsReceiver].scope("finagle").provideGauge("build/revision") {
          convertBuildRevision(revision).toFloat
        }
    }
  }

  /* Private */

  private[server] def convertBuildRevision(revision: String): Long = {
    try {
      revision.take(10).toU64Long
    } catch {
      case NonFatal(e) =>
        error(s"Unable to convert Finagle build revision to long: ${e.getMessage}")
        -1L
    }
  }
}
