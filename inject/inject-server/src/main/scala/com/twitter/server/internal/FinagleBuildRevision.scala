package com.twitter.server.internal

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Injector
import com.twitter.util.logging.Logging
import scala.util.control.NonFatal

object FinagleBuildRevision extends Logging {

  def register(injector: Injector): Unit = {
    com.twitter.finagle.Init.finagleBuildRevision match {
      case "?" =>
        warn("Unable to resolve Finagle revision.")
      case revision =>
        debug(s"Resolved Finagle build revision: (rev=$revision)")
        injector.instance[StatsReceiver].scope("finagle").provideGauge("build/revision") {
          convertBuildRevision(revision).toFloat
        }
    }
  }

  /* Private */

  /* exposed for testing */
  private[server] def convertBuildRevision(revision: String): Long = {
    try {
      java.lang.Long.parseUnsignedLong(revision.take(10), 16)
    } catch {
      case NonFatal(e) =>
        error(s"Unable to convert Finagle build revision to long: ${e.getMessage}")
        -1L
    }
  }
}
