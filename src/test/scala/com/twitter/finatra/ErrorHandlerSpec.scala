package com.twitter.finatra

import com.twitter.finatra.test.FlatSpecHelper
import com.twitter.util.Future

class ErrorHandlerSpec extends FlatSpecHelper {

  case class TheException() extends Exception

  class HandlingCtrl extends Controller {
    get("/handled") { request =>
      Future.exception(TheException())
    }

    error { request =>
      request.error match {
        case Some(TheException()) => render.ok.toFuture
        case _ => render.internalServerError.toFuture
      }
    }
  }

  class FailingCtrl extends Controller {
    get("/unhandled") { request =>
      Future.exception(TheException())
    }

    // We still need to specify an error handler, otherwise, we fallback on the other controller's handler
    // Fixing that requires changes to the API.
    error { request =>
      request.error match {
        case _ => render.internalServerError.toFuture
      }
    }
  }

  val server = new FinatraServer
  server.register(new HandlingCtrl)
  server.register(new FailingCtrl)

  "ErrorHandler" should "handle exceptions" in {
    get("/handled")
    response.code should equal (200)
    get("/unhandled")
    response.code should equal (500)
  }
}
