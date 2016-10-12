package com.twitter.finatra.http.tests.integration.doeverything.main.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.routing.Prefix

class PrefixedController extends Controller {
  get("/test") { _: Request => response.ok }
  prefix[TestPrefix].get("/test") { _: Request => response.ok }
}

class TestPrefix extends Prefix {
  def prefix: String = "/prefix"
}
