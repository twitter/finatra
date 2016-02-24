package com.twitter.finatra.http.integration.messagebody.main.domain

import com.twitter.finatra.request.QueryParam

case class GreetingRequest(
  @QueryParam name: String)
