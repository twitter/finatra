package com.twitter.finatra.http.tests.server.views

import com.twitter.finatra.http.annotations.Mustache

@Mustache(
  value = "testJson",
  contentType = "application/json; charset=utf-8"
)
case class JSONView(name: String)
