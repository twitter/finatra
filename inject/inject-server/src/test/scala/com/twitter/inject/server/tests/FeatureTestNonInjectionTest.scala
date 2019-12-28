package com.twitter.inject.server.tests

import com.twitter.finagle.http.Status
import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest}

/** Test a non-inject TwitterServer with the [[FeatureTest]] trait */
class FeatureTestNonInjectionTest extends FeatureTest {

  override val server: EmbeddedTwitterServer =
    new EmbeddedTwitterServer(
      twitterServer = new NonInjectionTestServer(),
      args = Seq("http.port=:0"),
      disableTestLogging = true)

  /* Does not work since we do not provide an InMemoryStatsReceiver but should not result in failures */
  override val printStats: Boolean = true

  /**
   * Explicitly start the server before all tests, close will be attempted
   * by [[com.twitter.inject.server.FeatureTestMixin]] in `afterAll`.
   */
  override def beforeAll(): Unit = {
    server.start()
  }

  test("TestServer#starts up") {
    server.assertHealthy()
  }

  test("TwitterServer#stats receivers") {
    // for a non-injectable server that has provided no SR override
    // we have no way of providing anything useful here
    intercept[IllegalStateException] {
      server.statsReceiver
    }
    // same as above, which means we also can't provide an in memory stats receiver
    intercept[IllegalStateException] {
      server.inMemoryStatsReceiver
    }
  }

  test("TestServer#feature test") {
    server.httpGetAdmin(
      "/admin/lint.json",
      andExpect = Status.Ok
    )

    server.httpGetAdmin(
      "/admin/registry.json",
      andExpect = Status.Ok
    )
  }

}
