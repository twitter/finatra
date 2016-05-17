package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.request.RouteParam
import com.twitter.finatra.validation.Size

case class TestUserWithGroupIdFromRoute(
  @RouteParam groupId: Long,
  @Size(min = 2, max = 20) name: String)
