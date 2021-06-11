package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.util.WrappedValue

case class RequestWithSeqWrappedString(value: Seq[WrappedString])

case class WrappedString(foo: String) extends WrappedValue[String]
