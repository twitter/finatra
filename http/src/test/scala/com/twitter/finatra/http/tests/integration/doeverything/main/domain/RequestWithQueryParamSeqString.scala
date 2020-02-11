package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.http.annotations.QueryParam

case class RequestWithQueryParamSeqString(@QueryParam foo: Seq[String])

case class ResponseOfQueryParamSeqString(foo: Seq[String])

case class RequestWithDefaultedQueryParamSeqString(
  @QueryParam foo: Seq[String] = Seq("foo", "bar", "baz")
)

case class RequestWithDefaultQueryParam(@QueryParam param: String = "default")

case class RequestWithQueryParamSeqLong(@QueryParam foo: Seq[Long])

case class RequestWithCommaSeparatedQueryParamSeqLong(@QueryParam(commaSeparatedList = true) foo: Seq[Long])

case class RequestWithUselessCommaSeparatedQueryParamLong(@QueryParam(commaSeparatedList = true) foo: Long)

case class ResponseOfQueryParamSeqLong(foo: Seq[Long])

case class RequestWithIntQueryParams(@QueryParam param: Seq[Int])

case class RequestWithShortQueryParams(@QueryParam param: Seq[Short])

case class RequestWithBooleanQueryParams(@QueryParam param: Seq[Boolean])

case class RequestWithBooleanNamedQueryParam(@QueryParam("foo") param: String)

case class RequestWithOptionBooleanQueryParam(@QueryParam param: Option[Boolean])

case class RequestWithBooleanQueryParam(@QueryParam param: Boolean)

case class RequestWithCaseClassQueryParams(@QueryParam param: Seq[AnotherCaseClass])

case class AnotherCaseClass(foo: String)
