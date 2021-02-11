package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.inject.domain.WrappedValue

case class RequestWithSeqWrappedString(value: Seq[WrappedString])

case class WrappedString(foo: String) extends WrappedValue[String]
