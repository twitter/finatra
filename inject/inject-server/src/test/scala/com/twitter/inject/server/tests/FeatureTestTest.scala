package com.twitter.inject.server.tests

import com.google.inject.testing.fieldbinder.Bind
import com.twitter.inject.server.{EmbeddedTwitterServer, WordSpecFeatureTest, TwitterServer}

class FeatureTestTest extends WordSpecFeatureTest {

  @Bind
  val string: String = "helloworld"

  override val server = new EmbeddedTwitterServer(new TwitterServer {})

  "feature test" in {
    server.injector.instance[String] should be("helloworld")
  }
}


