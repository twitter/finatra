package com.twitter.server.internal

import com.twitter.server.Lifecycle.PromoteToOldGen

/**
 * This util is needed to gain access to the private PromoteToOldGen.
 * We can't access this functionality through warmupComplete since our desired startup order is:
 * - Run app specific warmup
 * - PromoteToOldGen
 * - Start external HTTP or Thrift server
 * - Enable /health endpoint
 *
 * TODO: Remove need for this util
 */
object PromoteToOldGenUtils {

  private val promoteToOldGen = new PromoteToOldGen()

  def beforeServing() = {
    promoteToOldGen.beforeServing()
  }
}
