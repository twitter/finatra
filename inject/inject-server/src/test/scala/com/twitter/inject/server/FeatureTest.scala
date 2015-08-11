package com.twitter.inject.server

import com.twitter.inject.app.EmbeddedApp
import com.twitter.util.{Await, Future}

trait FeatureTest extends com.twitter.inject.app.FeatureTest {

  def printStats = true

  override protected def afterEach() {
    super.afterEach()
    if (server.isGuiceApp) {
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

  /* Allow the app to be named a "server" in feature-tests */
  protected def server: EmbeddedTwitterServer

  override protected def app: EmbeddedApp = server

  implicit class RichFuture[T](future: Future[T]) {
    def value: T = {
      Await.result(future)
    }
  }
}
