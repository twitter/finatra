package com.twitter.inject.server

import com.twitter.inject.app.{EmbeddedApp, IntegrationTest}

trait FeatureTest extends IntegrationTest {

  override protected def afterEach() {
    super.afterEach()
    if (server.isGuiceApp) {
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
}
