package com.twitter.inject.server.tests

import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTestMockito2, TwitterServer}
import org.scalatest.Tag

class FeatureTestMockito2Test extends FeatureTestMockito2 {

  override val server =
    new EmbeddedTwitterServer(new TwitterServer {})
    .bind[String]("helloworld")

  test("feature test") {
    server.injector.instance[String] should be("helloworld")
  }
}
