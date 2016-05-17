package com.twitter.finatra.http.tests.integration.multiserver.add2server

import com.twitter.finatra.httpclient.modules.HttpClientModule

object Add1HttpClientModule extends HttpClientModule {
  val dest = "flag!add1-server"
}