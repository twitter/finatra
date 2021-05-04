package com.twitter.finatra.http.tests.integration.tweetexample.main.domain

import com.twitter.finatra.validation.constraints.Min
import com.twitter.util.validation.MethodValidation
import com.twitter.util.validation.engine.MethodValidationResult

case class TweetRequest(@Min(1) customId: Long, username: String, tweetMsg: String) {

  @MethodValidation
  def fooCheck: MethodValidationResult = {
    MethodValidationResult.validIfTrue(username != "foo", "username cannot be foo")
  }
}
