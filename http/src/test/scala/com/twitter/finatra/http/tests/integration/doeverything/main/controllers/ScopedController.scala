package com.twitter.finatra.http.tests.integration.doeverything.main.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.routing.Scope

/**
 * Created by papacharlie on 10/7/16.
 */
class ScopedController extends Controller {
  get("/scope") { _: Request => response.ok }
}

class TestScope extends Scope {
  def prefix: String = "/test"
}

