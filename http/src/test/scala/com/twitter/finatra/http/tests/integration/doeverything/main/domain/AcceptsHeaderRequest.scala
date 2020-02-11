package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.http.annotations.Header

case class AcceptsHeaderRequest(
  @Header accept: String,
  @Header("accept-charset") acceptCharset: String,
  @Header("Accept-Charset") acceptCharsetAgain: String,
  @Header("Accept-Encoding") acceptEncoding: String
)
