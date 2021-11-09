package com.twitter.inject.server.tests

import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.inject.server.FeatureTest
import com.twitter.inject.server.TwitterServer

/** Test the `afterAll`` hooks of the [[FeatureTest]] trait */
class FeatureTestAfterAllTest extends FeatureTest {

  private[this] var hasAfterAllRun = false
  private[this] var hasAfterAllSuppressedException = false
  runAfterAll { hasAfterAllRun = true }
  runAfterAll { throw new IllegalStateException("This shouldn't block other 'afterAll' hooks") }
  runAfterAll { hasAfterAllSuppressedException = true }

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

  /**
   * We explicitly verify that we have run our `afterAll` and custom hooks.
   * If any of these assertions fails, a [[org.scalatest.exceptions.TestFailedException]]
   * will be thrown and the test will be considered a failure.
   */
  override def afterAll(): Unit = {
    assert(hasAfterAllRun == false)
    assert(hasAfterAllSuppressedException == false)
    assert(server.closed == false)
    super.afterAll() // we run all of the `afterAll` hooks here
    assert(hasAfterAllRun == true)
    assert(hasAfterAllSuppressedException)
    assert(server.closed)
  }

  test("TwitterServer#starts up") {
    server.assertHealthy()
  }

}
