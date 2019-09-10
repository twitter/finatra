package com.twitter.inject.server

import com.twitter.inject.{Injector, IntegrationTestMixin}
import org.scalatest.{Suite, SuiteMixin}
import scala.util.control.NonFatal

/**
 * Testing trait which extends the [[com.twitter.inject.IntegrationTestMixin]] to provide
 * utilities for [[https://twitter.github.io/finatra/user-guide/testing/#feature-tests Feature testing]]
 * with a test-defined [[com.twitter.inject.server.EmbeddedTwitterServer]] or subclass thereof.
 *
 * This trait is expected to be mixed with a class that extends a core Suite trait,
 * e.g., [[org.scalatest.FunSuite]].
 *
 * While you can use this mixin directly, it is recommended that users extend
 * the [[com.twitter.inject.server.FeatureTest]] abstract class.
 *
 * @see [[com.twitter.inject.IntegrationTestMixin]]
 */
trait FeatureTestMixin extends SuiteMixin with IntegrationTestMixin { this: Suite =>

  protected def server: EmbeddedTwitterServer

  override protected def injector: Injector = server.injector

  def printStats: Boolean = false

  override protected def afterEach(): Unit = {
    super.afterEach()
    try {
      if (server.usesInMemoryStatsReceiver) {
        if (printStats) {
          server.printStats()
        }
        server.clearStats()
      }
    } catch {
      case _: IllegalStateException => /* DO NOTHING */
      // we don't have access to a StatsReceiver to perform these functions for the user
    }
  }

  override protected def afterAll(): Unit = {
    try {
      super.afterAll()
    } finally {
      server.close()
      try {
        server.assertCleanShutdown()
      } catch {
        case NonFatal(e) =>
          e.printStackTrace(System.err)
      }
    }
  }
}
