package com.twitter.inject.server

import com.twitter.inject.{Injector, IntegrationTest, Test}
import com.twitter.util.{Await, Future}

trait FeatureTest extends Test with IntegrationTest {

  protected def server: EmbeddedTwitterServer

  override protected def injector: Injector = server.injector

  def printStats = true

  override protected def beforeAll() {
    if (server.isStarted && hasBoundFields) {
      throw new Exception("ERROR: Server started before integrationTestModule added. " +
        "@Bind will not work unless references to the server are lazy, or within a ScalaTest " +
        "lifecycle method or test method, or the integrationTestModule is manually added as " +
        "an override module.")
    }

    assert(server.isInjectable)
    server.injectableServer.addFrameworkOverrideModules(integrationTestModule)
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
