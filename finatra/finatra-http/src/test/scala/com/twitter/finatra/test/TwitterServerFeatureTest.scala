package com.twitter.finatra.test

trait TwitterServerFeatureTest
  extends IntegrationTest {

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
