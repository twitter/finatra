package com.twitter.inject

package object server {
  private[twitter] def nonEmpty(s: String): Boolean = s != null && s.nonEmpty

  private[twitter] def nonEmpty(s: Seq[_]): Boolean = s != null && s.nonEmpty

  /**
   * NOTE: We avoid using slf4j-api info logging so that we can differentiate the
   * underlying server logs from the testing framework logging without requiring a
   * test logging configuration to be loaded.
   *
   * @param str the string message to log
   * @param suppress if writing the line should be suppressed
   */
  private[twitter] def info(str: String, suppress: Boolean = false): Unit = {
    if (!suppress) {
      println(str)
    }
  }

  /**
   * NOTE: We avoid using slf4j-api info logging so that we can differentiate the
   * underlying server logs from the testing framework logging without requiring a
   * test logging configuration to be loaded.
   *
   * @param str the string message to log
   * @param suppress if writing the line should be suppressed
   */
  private[twitter] def infoBanner(str: String, suppress: Boolean = false): Unit = {
    info("\n", suppress)
    info("=" * 75, suppress)
    info(str, suppress)
    info("=" * 75, suppress)
  }
}
