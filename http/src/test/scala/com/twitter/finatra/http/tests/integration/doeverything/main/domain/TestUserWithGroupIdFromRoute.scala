package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.http.annotations.RouteParam
import com.twitter.finatra.validation.constraints.Size

case class TestUserWithGroupIdFromRoute(
  @RouteParam groupId: Long,
  @Size(min = 2, max = 20) name: String)
