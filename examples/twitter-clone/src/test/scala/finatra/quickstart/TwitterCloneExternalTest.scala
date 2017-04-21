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

  test("tweet creation") {
    pending

    /* Typically, we would create the server once outside of any individual test
       case since there is a non-zero startup cost to creating a server. In this
       case, however, we have single test case AND we do not want the server to
       start UNLESS this test case is run, therefore we move all access to the
       server into this single test method -- which is marked `pending` as it
       should only ever be run manually and not within any continuous
       integration workflow.*/

    val server = new EmbeddedHttpServer(
      new TwitterCloneServer,
      flags = Map(
        "firebase.host" -> "finatra.firebaseio.com",
        "com.twitter.server.resolverMap" -> "firebase=finatra.firebaseio.com:443"))
    try {
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
    } finally {
      server.close()
    }
  }
}
