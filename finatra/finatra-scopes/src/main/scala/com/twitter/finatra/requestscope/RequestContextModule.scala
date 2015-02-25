package com.twitter.finatra.requestscope

import com.twitter.finagle.http.Request

object RequestContextModule extends RequestScopeBinding {
  override def configure(): Unit = {
    bindRequestScope[Request.Schema.Record]
  }
}
