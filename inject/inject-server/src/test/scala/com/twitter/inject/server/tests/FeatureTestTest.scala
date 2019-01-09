package com.twitter.inject.server.tests

import com.twitter.finagle.http.Status
import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest, TwitterServer}

/** Test an injectable TwitterServer with the [[FeatureTest]] trait */
class FeatureTestTest extends FeatureTest {

  /* Disable printing of stats for injectable TwitterServer under test */
  override val printStats = false

  override val server: EmbeddedTwitterServer =
    new EmbeddedTwitterServer(
      twitterServer = new TwitterServer {},
      disableTestLogging = true
    ).bind[String].toInstance("helloworld")

  /**
   * Explicitly start the server before all tests, close will be attempted by
   * [[com.twitter.inject.server.FeatureTestMixin]] in `afterAll`.
   */
  override def beforeAll(): Unit = {
    server.start()
  }

  test("TwitterServer#starts up") {
    server.assertHealthy()
  }

  test("TwitterServer#feature test") {
    server.httpGetAdmin(
      "/admin/lint.json",
      andExpect = Status.Ok
    )

    server.httpGetAdmin(
      "/admin/registry.json",
      andExpect = Status.Ok
    )
  }

  test("TwitterServer#bind test") {
    server.injector.instance[String] should be("helloworld")
  }
}
