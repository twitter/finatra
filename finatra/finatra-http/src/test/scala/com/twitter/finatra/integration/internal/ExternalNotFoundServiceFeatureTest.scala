package com.twitter.finatra.integration.internal

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.FinatraServer
import com.twitter.finatra.response.SimpleResponse
import com.twitter.finatra.test.{EmbeddedTwitterServer, HttpTest}
import com.twitter.finatra.twitterserver.routing.Router
import com.twitter.util.Future

class ExternalNotFoundServiceFeatureTest extends HttpTest {

  "raw server" in {
    val server = EmbeddedTwitterServer(ExternalNotFoundServer)
    server.httpGet(
      "/",
      andExpect = Status.Ok,
      withBody = "hi")
  }

  object ExternalNotFoundServer extends FinatraServer {

    override def configure(router: Router) {
      val service = new Service[Request, Response] {
        def apply(request: Request) = {
          Future(SimpleResponse(Status.Ok, "hi"))
        }
      }

      router.
        externalNotFoundService(service)
    }
  }

}