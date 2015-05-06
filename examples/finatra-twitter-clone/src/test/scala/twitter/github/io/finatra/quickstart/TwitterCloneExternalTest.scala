package twitter.github.io.finatra.quickstart

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.test.{EmbeddedHttpServer, HttpTest}
import finatra.quickstart.TwitterCloneServer

class TwitterCloneExternalTest extends HttpTest {

  val server = new EmbeddedHttpServer(
    new TwitterCloneServer,
    clientFlags = Map(
      "firebase.host" -> "finatra.firebaseio.com",
      "com.twitter.server.resolverMap" -> "firebase=finatra.firebaseio.com:443"))

  "tweet creation" in {
    pending
    val result = server.httpPost(
      path = "/tweet",
      postBody =
        """
        {
          "message": "Hello #SFScala",
          "lat": "37.7821120598956",
          "long": "-122.400612831116",
          "sensitive": false
        }
        """,
      andExpect = Created,
      withJsonBody =
        """
        {
          "id": "0",
          "message": "Hello #SFScala",
          "lat": "37.7821120598956",
          "long": "-122.400612831116",
          "sensitive": false
        }
        """,
      withJsonBodyNormalizer = idNormalizer)

    server.httpGet(
      path = result.location.get,
      andExpect = Ok,
      withJsonBody = result.contentString)
  }
}