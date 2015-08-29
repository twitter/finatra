package twitter.github.io.finatra.quickstart

import com.google.inject.testing.fieldbinder.Bind
import com.twitter.finagle.httpx.Status._
import com.twitter.finatra.http.test.{EmbeddedHttpServer, HttpTest}
import com.twitter.inject.Mockito
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future
import finatra.quickstart.TwitterCloneServer
import finatra.quickstart.domain.TweetId
import finatra.quickstart.domain.http.{TweetResponse, TweetLocation}
import finatra.quickstart.firebase.FirebaseClient
import finatra.quickstart.services.IdService

class TwitterCloneFeatureTest extends FeatureTest with Mockito with HttpTest {

  override val server = new EmbeddedHttpServer(new TwitterCloneServer)

  @Bind val firebaseClient = smartMock[FirebaseClient]

  @Bind val idService = smartMock[IdService]

  /* Mock GET Request performed in TwitterCloneWarmup */
  firebaseClient.get("/tweets/123.json")(manifest[TweetResponse]) returns Future(None)

  "tweet creation" in {
    idService.getId returns Future(TweetId("123"))

    val savedStatus = TweetResponse(
      id = TweetId("123"),
      message = "Hello #FinagleCon",
      location = Some(TweetLocation(37.7821120598956, -122.400612831116)),
      nsfw = false)

    firebaseClient.put("/tweets/123.json", savedStatus) returns Future.Unit
    firebaseClient.get("/tweets/123.json")(manifest[TweetResponse]) returns Future(Option(savedStatus))
    firebaseClient.get("/tweets/124.json")(manifest[TweetResponse]) returns Future(None)
    firebaseClient.get("/tweets/125.json")(manifest[TweetResponse]) returns Future(None)

    val result = server.httpPost(
      path = "/tweet",
      postBody = """
        {
          "message": "Hello #FinagleCon",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "nsfw": false
        }""",
      andExpect = Created,
      withJsonBody = """
        {
          "id": "123",
          "message": "Hello #FinagleCon",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "nsfw": false
        }""")

    server.httpGetJson[TweetResponse](
      path = result.location.get,
      andExpect = Ok,
      withJsonBody = result.contentString)

    server.httpPost(
      path = "/tweet/lookup",
      postBody = """[123,124,125,123]""",
      andExpect = Ok,
      withJsonBody = """
      [
        {
          "id": "123",
          "message": "Hello #FinagleCon",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "nsfw": false
        },
        {
          "id": "123",
          "message": "Hello #FinagleCon",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "nsfw": false
        }
      ]
      """)
  }

  "Post bad tweet" in {
    server.httpPost(
      path = "/tweet",
      postBody = """
        {
          "message": "",
          "location": {
            "lat": "9999"
          },
          "nsfw": "abc"
        }""",
      andExpect = BadRequest,
      withJsonBody = """
        {
          "errors" : [
            "message: size [0] is not between 1 and 140",
            "location.lat: [9999.0] is not between -85 and 85",
            "location.long: field is required",
            "nsfw: 'abc' is not a valid boolean"
          ]
        }
        """)
  }
}
