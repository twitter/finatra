package com.twitter.finatra.integration.doeverything.main.domain

import com.twitter.finatra.request._

case class RequestWithQueryParamSeqString(@QueryParam foo: Seq[String])

case class ResponseOfQueryParamSeqString(foo: Seq[String])

case class RequestWithQueryParamSeqLong(@QueryParam foo: Seq[Long])

case class ResponseOfQueryParamSeqLong(foo: Seq[Long])
