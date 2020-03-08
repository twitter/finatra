package com.twitter.finatra.http.tests.integration.messagebody.main.domain

import com.twitter.finatra.http.annotations.QueryParam

case class GreetingRequest(@QueryParam name: String)
