package finatra.quickstart

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class TwitterCloneStartupTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new TwitterCloneServer,
    flags = Map(
      "com.twitter.server.resolverMap" -> "firebase=nil!"))

  "server" in {
    server.assertHealthy()
  }
}
