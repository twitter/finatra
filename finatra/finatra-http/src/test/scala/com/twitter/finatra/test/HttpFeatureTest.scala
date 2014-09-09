package com.twitter.finatra.test

trait HttpFeatureTest
  extends IntegrationTest
  with HttpTest {

  /* Allow the app to be called a "server" in feature-tests */
  protected def server: EmbeddedTwitterServer

  override protected def app: EmbeddedApp = server
}
