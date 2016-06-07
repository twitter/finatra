package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.domain.WrappedValue
import com.twitter.finatra.validation.Min

case class ValidatedWrappedLong(
  @Min(1) value: Long)
  extends WrappedValue[Long]
