package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.request.QueryParam
import com.twitter.finatra.validation.NotEmpty

case class SomethingStreamedRequest(
  @NotEmpty @QueryParam somethingId: String,
  @QueryParam field1: Option[String],
  @QueryParam field2: Option[Int])