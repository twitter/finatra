package com.twitter.finatra.integration.tweetexample.main.domain

import com.twitter.finatra.validation.{MethodValidation, Min, ValidationResult}

case class TweetRequest(
  @Min(1) customId: Long,
  username: String,
  tweetMsg: String) {

  @MethodValidation
  def fooCheck = {
    ValidationResult(
      username != "foo",
      "username cannot be foo")
  }
}
