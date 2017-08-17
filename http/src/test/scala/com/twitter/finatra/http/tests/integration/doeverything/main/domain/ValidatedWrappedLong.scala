package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.validation.Min
import com.twitter.inject.domain.WrappedValue

case class ValidatedWrappedLong(@Min(1) value: Long) extends WrappedValue[Long]
