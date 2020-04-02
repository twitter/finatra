package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.http.annotations.QueryParam
import com.twitter.finatra.validation.constraints.NotEmpty

case class SomethingStreamedRequest(
  @NotEmpty @QueryParam somethingId: String,
  @QueryParam field1: Option[String],
  @QueryParam field2: Option[Int])
