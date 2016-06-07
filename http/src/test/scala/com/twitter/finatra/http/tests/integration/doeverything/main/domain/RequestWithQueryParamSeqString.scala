package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.request.QueryParam

case class RequestWithQueryParamSeqString(
  @QueryParam foo: Seq[String])

case class ResponseOfQueryParamSeqString(
  foo: Seq[String])

case class RequestWithQueryParamSeqLong(
  @QueryParam foo: Seq[Long])

case class ResponseOfQueryParamSeqLong(
  foo: Seq[Long])

case class RequestWithIntQueryParams(
  @QueryParam param: Seq[Int])

case class RequestWithShortQueryParams(
  @QueryParam param: Seq[Int])

case class RequestWithBooleanQueryParams(
  @QueryParam param: Seq[Boolean])

case class RequestWithOptionBooleanQueryParam(
  @QueryParam param: Option[Boolean])

case class RequestWithBooleanQueryParam(
  @QueryParam param: Boolean)

case class RequestWithCaseClassQueryParams(
  @QueryParam param: Seq[AnotherCaseClass])

case class AnotherCaseClass(
  foo: String)
