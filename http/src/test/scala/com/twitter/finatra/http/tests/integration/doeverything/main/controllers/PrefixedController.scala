package com.twitter.finatra.http.tests.integration.doeverything.main.controllers

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.routing.{MergedPrefix, Prefix}

class PrefixedController extends Controller {
  get("/test") { _: Request => response.ok }
  prefix[TestPrefix].get("/test") { _: Request => response.ok }
  prefix[TestMergedPrefix].get("") { _: Request => response.ok }
}

class TestPrefix extends Prefix {
  def prefix: String = "/prefix"
}

class OtherTestPrefix extends Prefix {
  def prefix: String = "/other"
}

class TestMergedPrefix @Inject()(
  a: TestPrefix,
  b: OtherTestPrefix
) extends MergedPrefix(a, b)
