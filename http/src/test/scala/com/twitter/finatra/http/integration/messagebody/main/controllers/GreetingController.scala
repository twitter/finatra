package com.twitter.finatra.http.integration.messagebody.main.controllers

import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.integration.messagebody.main.domain.GreetingRequest
import javax.inject.Inject

class GreetingController @Inject()()
  extends Controller {

  get("/greet") { gr: GreetingRequest =>
    gr
  }
}
