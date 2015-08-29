package com.twitter.finatra.http

import com.twitter.finatra.http.response._
import com.twitter.inject.Logging
import javax.inject.Inject

abstract class Controller extends RouteDSL with Logging {
  /*
   * NOTE: Using constructor-injection for the following fields would add boilerplate to all
   * controllers, so instead we use vars to allow setter injection after object creation
   */
  @Inject private var responseBuilder: ResponseBuilder = _

  /* Protected */
  protected def response = responseBuilder
}
