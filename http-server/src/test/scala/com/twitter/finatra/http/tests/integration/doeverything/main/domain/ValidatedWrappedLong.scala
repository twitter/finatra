package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.validation.constraints.Min
import com.twitter.util.WrappedValue

case class ValidatedWrappedLong(@Min(1) value: Long) extends WrappedValue[Long]
