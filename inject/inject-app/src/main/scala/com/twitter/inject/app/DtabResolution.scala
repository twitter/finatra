package com.twitter.inject.app

import com.twitter.finagle.{Dtab, DtabFlags}

/**
 * When using the `dtab.add` Flag defined in [[com.twitter.finagle.DtabFlags]]
 * users can mix in this trait to add the ability to correctly apply supplemental
 * delegation tables to their base Dtab.
 *
 * @see [[com.twitter.finagle.Dtab]]
 * @see [[com.twitter.finagle.DtabFlags]]
 */
trait DtabResolution { self: App with DtabFlags =>

  /** Ensure we apply any supplemental delegation table(s) */
  premain {
    this.addDtabs()
    val entries = for (entry <- Dtab.base) yield { "\t" + entry.show }
    info("Installed Dtab:\n" + entries.mkString(";\n"))
  }

}
