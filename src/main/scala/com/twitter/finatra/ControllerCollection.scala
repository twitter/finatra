package com.twitter.finatra

import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}

class ControllerCollection {
  var controllers: Seq[Controller] = Seq.empty

  var notFoundHandler = { request:Request =>
    render.status(404).plain("Not Found").toFuture
  }

  var errorHandler = { request:Request =>
    render.status(500).plain("Something went wrong!").toFuture
  }

  def render = new Response

  def dispatch(request: FinagleRequest):Option[FinagleResponse] = {
    var response:Option[FinagleResponse] = None

    controllers.find { ctrl =>
      ctrl.dispatch(request) match {
        case Some(callbackResponse) =>
          response = Some(callbackResponse)
          true
        case None =>
          false
      }
    }

    response
  }

  def add(controller: Controller) {
    notFoundHandler = controller.notFoundHandler.getOrElse(notFoundHandler)
    errorHandler    = controller.errorHandler.getOrElse(errorHandler)
    controllers     = controllers ++ Seq(controller)
  }

}
