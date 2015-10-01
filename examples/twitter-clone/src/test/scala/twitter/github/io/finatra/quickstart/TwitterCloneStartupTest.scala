package twitter.github.io.finatra.quickstart

import com.google.inject.Stage
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import finatra.quickstart.TwitterCloneServer

class TwitterCloneStartupTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new TwitterCloneServer,
    clientFlags = Map(
      "com.twitter.server.resolverMap" -> "firebase=nil!"))

  "server" in {
    server.assertHealthy()
  }
}