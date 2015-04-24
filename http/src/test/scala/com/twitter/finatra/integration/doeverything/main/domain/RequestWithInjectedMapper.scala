package com.twitter.finatra.integration.doeverything.main.domain

import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.request._

case class RequestWithInjectedMapper(
  @RequestInject mapper: FinatraObjectMapper,
  @QueryParam foo: Option[String])