package finatra.quickstart

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.Test
import finatra.quickstart.domain.http.TweetResponse

class TwitterCloneExternalTest extends Test {

  val NormalizedId = "0"

  def idNormalizer(jsonNode: JsonNode): JsonNode = {
    val objNode = jsonNode.asInstanceOf[ObjectNode]
    if (objNode.has("id")) {
      objNode.put("id", NormalizedId)
    }
    objNode
  }

  val server = new EmbeddedHttpServer(
    new TwitterCloneServer,
    flags = Map(
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
