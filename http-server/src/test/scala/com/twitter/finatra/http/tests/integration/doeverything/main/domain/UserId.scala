package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.util.WrappedValue

case class UserId(id: Long) extends WrappedValue[Long]
