package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.domain.WrappedValue

case class UserId(
   id: Long)
  extends WrappedValue[Long]
