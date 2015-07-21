package twitter.github.io.finatra.quickstart

import com.google.inject.testing.fieldbinder.Bind
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.Mockito
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future
import finatra.quickstart.TwitterCloneServer
import finatra.quickstart.domain.{Status, StatusId}
import finatra.quickstart.firebase.FirebaseClient
import finatra.quickstart.services.IdService

class TwitterCloneFeatureTest extends FeatureTest with Mockito {

  override val server = new EmbeddedHttpServer(
    twitterServer = new TwitterCloneServer {
      override val overrideModules = Seq(integrationTestModule)
    })

  @Bind val firebaseClient = smartMock[FirebaseClient]

  @Bind val idService = smartMock[IdService]

  "tweet creation" in {
    idService.getId returns Future(StatusId("123"))

    val mockStatus = Status(
      id = StatusId("123"),
      text = "Hello #SFScala",
      lat = Some(37.7821120598956),
      long = Some(-122.400612831116),
      sensitive = false)

    firebaseClient.put("/statuses/123.json", mockStatus) returns Future.Unit
    firebaseClient.get("/statuses/123.json")(manifest[Status]) returns Future(Option(mockStatus))

    val result = server.httpPost(
      path = "/tweet",
      postBody = """
        {
          "message": "Hello #SFScala",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "sensitive": false
        }""",
      andExpect = Created,
      withJsonBody = """
        {
          "id": "123",
          "message": "Hello #SFScala",
          "location": {
            "lat": "37.7821120598956",
            "long": "-122.400612831116"
          },
          "sensitive": false
        }""")

    server.httpGet(
      path = result.location.get,
      andExpect = Ok,
      withJsonBody = result.contentString)
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
          "sensitive": "abc"
        }""",
      andExpect = BadRequest,
      withJsonBody = """
        {
          "errors" : [
            "message size [0] is not between 1 and 140",
            "location.lat [9999.0] is not between -85 and 85",
            "location.long is a required field",
            "sensitive's value 'abc' is not a valid boolean"
          ]
        }
        """)
  }
}