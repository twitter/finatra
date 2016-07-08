package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.request.Header

case class UserAgentRequest(
   @Header `user-agent`: String)
