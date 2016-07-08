package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.request.QueryParam
import com.twitter.finatra.test.Prod
import javax.inject.Inject

case class RequestWithInjections(
  @QueryParam id: UserId,
  @QueryParam id2: Option[UserId],
  @QueryParam id3: Option[Int],
  @QueryParam id4: Option[Int],
  @Inject defaultString: String,
  @Prod @Inject defaultProdString: String,
  @Inject defaultOptString: Option[String],
  @Prod @Inject defaultOptProdString: Option[String])
