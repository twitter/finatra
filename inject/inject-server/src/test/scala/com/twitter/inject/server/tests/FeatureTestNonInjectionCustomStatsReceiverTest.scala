package com.twitter.inject.server.tests

import com.twitter.finagle.http.Status
import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest}

/** Test a non-inject TwitterServer using a Custom StatsReceiver implementation with the [[FeatureTest]] trait */
class FeatureTestNonInjectionCustomStatsReceiverTest extends FeatureTest {

  private[this] val testStatsReceiver: TestStatsReceiver = new TestStatsReceiver

  override val server: EmbeddedTwitterServer =
    new EmbeddedTwitterServer(
      twitterServer = new NonInjectionTestServer(Some(testStatsReceiver)),
      args = Seq("http.port=:0"),
      disableTestLogging = true,
      statsReceiverOverride = Some(testStatsReceiver)
    )
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
    // even though the server under test is a non-injectable server, we
    // have been provided with a stats receiver override which we return here
    server.statsReceiver
    // but it is not in-memory stats receiver
    intercept[IllegalStateException] {
      server.inMemoryStatsReceiver
    }

    assert(testStatsReceiver.gauges.nonEmpty) /* we add a build revision gauge in startup of the server */
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
