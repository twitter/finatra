package com.twitter.finatra.http.integration.doeverything.main.domain

import com.twitter.finatra.request.{RequestInject, QueryParam}
import com.twitter.finatra.test.Prod

case class RequestWithInjections(
  @QueryParam id: UserId,
  @QueryParam id2: Option[UserId],
  @QueryParam id3: Option[Int],
  @QueryParam id4: Option[Int],
  @RequestInject defaultString: String,
  @Prod @RequestInject defaultProdString: String,
  @RequestInject defaultOptString: Option[String],
  @Prod @RequestInject defaultOptProdString: Option[String])
