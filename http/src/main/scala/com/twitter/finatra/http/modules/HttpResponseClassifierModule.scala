package com.twitter.finatra.http.modules

import com.twitter.finatra.http.response.HttpResponseClassifier
import com.twitter.inject.TwitterModule

object HttpResponseClassifierModule extends TwitterModule {

  override def configure(): Unit = {
    bind[HttpResponseClassifier].toInstance(
      HttpResponseClassifier.ServerErrorsAsFailures
    )
  }
}
