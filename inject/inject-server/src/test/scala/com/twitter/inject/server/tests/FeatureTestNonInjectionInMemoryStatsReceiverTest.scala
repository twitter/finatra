package com.twitter.inject.server.tests

import com.twitter.finagle.http.Status
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest}
import org.scalatest.concurrent.Eventually

/** Test a non-inject TwitterServer using an InMemoryStatsReceiver implementation with the [[FeatureTest]] trait */
class FeatureTestNonInjectionInMemoryStatsReceiverTest extends FeatureTest with Eventually {

  private[this] val inMemoryStatsReceiver: InMemoryStatsReceiver = new InMemoryStatsReceiver

  override val server: EmbeddedTwitterServer =
    new EmbeddedTwitterServer(
      twitterServer = new NonInjectionTestServer(Some(inMemoryStatsReceiver)),
      args = Seq("http.port=:0"),
      disableTestLogging = true,
      statsReceiverOverride = Some(inMemoryStatsReceiver)
    )
  /* Works as we provide an override that is an InMemoryStatsReceiver */
  override val printStats: Boolean = true

  /**
   * Explicitly start the server before all tests, close will be attempted
   * by [[com.twitter.inject.server.FeatureTestMixin]] in `afterAll`.
   */
  override def beforeAll(): Unit = {
    server.start()

    // the server starts and is marked as healthy as soon as the admin starts
    // we need a little time for the exposed server to start and setup stats
    eventually {
      assert(
        inMemoryStatsReceiver.gauges.nonEmpty
      ) /* we add a build revision gauge in startup of the server */
    }
  }

  test("TestServer#starts up") {
    server.assertHealthy()
  }

  test("TwitterServer#stats receivers") {
    // even though the server under test is a non-injectable server, we
    // have been provided with a stats receiver override which we return here
    server.statsReceiver
    // and the override is an in-memory stats receiver
    server.inMemoryStatsReceiver should be(inMemoryStatsReceiver)
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
