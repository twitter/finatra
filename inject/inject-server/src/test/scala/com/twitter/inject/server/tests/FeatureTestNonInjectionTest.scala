package com.twitter.inject.server.tests

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.http.Status
import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest}
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Duration}

/** Test a non-inject TwitterServer with the [[FeatureTest]] trait */
class FeatureTestNonInjectionTest extends FeatureTest {

  override val server: EmbeddedTwitterServer =
    new EmbeddedTwitterServer(
      twitterServer = new TestTwitterServer,
      disableTestLogging = true)

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

class TestTwitterServer extends TwitterServer {
  /* ensure enough time to close resources */
  override val defaultCloseGracePeriod: Duration = 15.seconds
  def main(): Unit = {
    /* injectable TwitterServer automatically awaits on the admin, we need to do it explicitly here.*/
    Await.ready(adminHttpServer)
  }
}
