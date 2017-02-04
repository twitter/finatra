package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.inject.domain.WrappedValue

case class UserId(
   id: Long)
  extends WrappedValue[Long]
