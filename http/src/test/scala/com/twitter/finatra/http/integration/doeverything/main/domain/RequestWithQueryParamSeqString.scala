package com.twitter.finatra.http.integration.doeverything.main.domain

import com.twitter.finatra.request.QueryParam

case class RequestWithQueryParamSeqString(@QueryParam foo: Seq[String])

case class ResponseOfQueryParamSeqString(foo: Seq[String])

case class RequestWithQueryParamSeqLong(@QueryParam foo: Seq[Long])

case class ResponseOfQueryParamSeqLong(foo: Seq[Long])
