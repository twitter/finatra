package com.twitter.inject.server

import com.twitter.inject.{Injector, IntegrationTestMixin}
import com.twitter.util.{Await, Future}
import org.scalatest.{Suite, SuiteMixin}

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

  def printStats = true

  override protected def beforeAll() {
    if (server.isStarted && hasBoundFields) {
      throw new Exception(
        "ERROR: Server started before integrationTestModule added. " +
          "@Bind will not work unless references to the server are lazy, or within a ScalaTest " +
          "lifecycle method or test method, or the integrationTestModule is manually added as " +
          "an override module."
      )
    }

    if (hasBoundFields) {
      assert(server.isInjectable)
      server.injectableServer.addFrameworkOverrideModules(integrationTestModule)
    }
    super.beforeAll()
  }

  override protected def afterEach() {
    super.afterEach()
    if (server.isInjectable) {
      if (printStats) {
        server.printStats()
      }
      server.clearStats()
    }
  }

  override protected def afterAll() {
    try {
      super.afterAll()
    } finally {
      server.close()
    }
  }

  implicit class RichFuture[T](future: Future[T]) {
    def value: T = {
      Await.result(future)
    }
  }

}
