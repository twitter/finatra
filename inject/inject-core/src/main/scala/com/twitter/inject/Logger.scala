package com.twitter.inject

import scala.reflect._

/**
 * A factory for retrieving a Scala wrapped SLF4JLogger
 * e.g. val logger = Logger[MyClass]
 */
@deprecated("Use com.twitter.util.logging.Logger", "20170215")
object Logger {
  def apply[C: ClassTag](): grizzled.slf4j.Logger = {
    grizzled.slf4j.Logger[C]
  }
}