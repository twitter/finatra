package com.twitter.finatra.http.tests.integration.tweetexample.main.domain

import com.twitter.finatra.validation.constraints.Min
import com.twitter.finatra.validation.{MethodValidation, ValidationResult}

case class TweetRequest(@Min(1) customId: Long, username: String, tweetMsg: String) {

  @MethodValidation
  def fooCheck = {
    ValidationResult.validate(username != "foo", "username cannot be foo")
  }
}
