package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.request.Header

case class CreateUserRequest(
  @Header requestId: String,
  name: String,
  age: Int)

