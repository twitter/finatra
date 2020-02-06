package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.request.QueryParam
import javax.inject.Inject

case class RequestWithInjectedMapper(
  @Inject mapper: ScalaObjectMapper,
  @QueryParam foo: Option[String]
)
