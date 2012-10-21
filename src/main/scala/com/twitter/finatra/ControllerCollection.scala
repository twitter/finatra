package com.twitter.finatra

import com.twitter.util.Future
import com.twitter.finagle.http.{Response => FinagleResponse}

class ControllerCollection {
  var ctrls: Seq[Controller] = Seq.empty

  def dispatch(request: Request):Option[FinagleResponse] = {
    var response:Option[FinagleResponse] = None
    ctrls.find { ctrl =>
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
    ctrls = ctrls ++ Seq(controller)
  }

}
