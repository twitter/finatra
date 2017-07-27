package com.twitter.inject.server.tests

import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest, TwitterServer}

class FeatureTestTest extends FeatureTest {

  override val server =
    new EmbeddedTwitterServer(new TwitterServer {})
      .bind[String]("helloworld")

  test("feature test") {
    server.injector.instance[String] should be("helloworld")
  }
}
