package twitter.github.io.finatra.quickstart

import com.twitter.finagle.httpx.Status._
import com.twitter.finatra.http.test.{EmbeddedHttpServer, HttpTest}
import finatra.quickstart.TwitterCloneServer
import finatra.quickstart.domain.http.TweetResponse

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
          "message": "Hello #FinagleCon",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "nsfw": false
        }
        """,
      andExpect = Created,
      withJsonBody =
        """
        {
          "id": "0",
          "message": "Hello #FinagleCon",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "nsfw": false
        }
        """,
      withJsonBodyNormalizer = idNormalizer)

    val tweet = server.httpGetJson[TweetResponse](
      path = result.location.get,
      andExpect = Ok,
      withJsonBody = result.contentString)

    println(s"Firebase Tweet: https://finatra.firebaseio.com/tweets/${tweet.id}")
  }
}
