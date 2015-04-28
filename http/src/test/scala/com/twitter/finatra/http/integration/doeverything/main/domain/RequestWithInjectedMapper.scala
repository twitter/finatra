package com.twitter.finatra.http.integration.doeverything.main.domain

import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.request.{RequestInject, QueryParam}

case class RequestWithInjectedMapper(
  @RequestInject mapper: FinatraObjectMapper,
  @QueryParam foo: Option[String])
