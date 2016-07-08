package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.request.QueryParam
import com.twitter.finatra.validation.{Max, PastTime}
import org.joda.time.DateTime

case class UsersRequest(
  @Max(100) @QueryParam max: Int,
  @PastTime @QueryParam startDate: Option[DateTime],
  @QueryParam verbose: Boolean = false)
