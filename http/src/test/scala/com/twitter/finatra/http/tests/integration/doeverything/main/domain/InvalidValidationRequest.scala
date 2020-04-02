package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.validation.constraints.Max

case class InvalidValidationRequest(
  // @Max is not applicable to Strings, use @Size(min, max)
  @Max(255) name: String
)
