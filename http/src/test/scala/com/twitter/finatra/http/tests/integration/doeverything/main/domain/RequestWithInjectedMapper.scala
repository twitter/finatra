package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.request.QueryParam
import javax.inject.Inject

case class RequestWithInjectedMapper(
  @Inject mapper: FinatraObjectMapper,
  @QueryParam foo: Option[String])
