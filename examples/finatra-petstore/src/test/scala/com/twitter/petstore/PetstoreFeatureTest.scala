package com.twitter.petstore

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class PetstoreFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new PetstoreServer)

  "PetstoreServer" should {
    //getPet
    "Return valid pets" in { randInt: Int =>
      server.httpGet(
        path = "/pet/" + randInt,
        andExpect = Ok
      )
    }
    "Fail to return invalid pets" in {
      server.httpGet(
        path = "/pet/100",
        andExpect = NotFound
      )
    }

    //addPet
    "Add valid pets" in {
      server.httpPost(
        path = "/pet",
        postBody =
          """
            |{
            |  "name": "Ell",
            |  "photoUrls": [],
            |  "category": {"name": "Wyverary"},
            |  "tags": [{"name": "Wyvern"}, {"name": "Library"}],
            |  "status": "pending"
            |}
          """.stripMargin,
        andExpect = Ok
//        withJsonBody =
//          """
//            |{
//            |  "name": "Ell",
//            |  "photoUrls": [],
//            |  "category": {"name": "Wyverary"},
//            |  "tags": [{"name": "Wyvern"}, {"name": "Library"}],
//            |  "status": "pending"
//            |}
//          """.stripMargin
      )
    }


    //updatePet

  }
}
