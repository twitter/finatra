package com.twitter.finatra.http.tests.integration.messagebody.main.domain

import com.twitter.finatra.request.QueryParam

case class GreetingRequest(
  @QueryParam name: String)
