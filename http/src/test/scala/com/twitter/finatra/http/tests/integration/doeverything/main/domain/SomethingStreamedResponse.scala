package com.twitter.finatra.http.tests.integration.doeverything.main.domain

case class SomethingStreamedResponse(
  somethingId: String,
  field1: Option[String],
  field2: Option[Int])
