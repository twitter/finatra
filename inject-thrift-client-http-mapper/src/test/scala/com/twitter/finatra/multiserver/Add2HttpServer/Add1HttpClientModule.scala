package com.twitter.finatra.multiserver.Add2HttpServer

import com.twitter.finatra.httpclient.modules.HttpClientModule

object Add1HttpClientModule extends HttpClientModule {
  val dest = "flag!add1-http-server"
}