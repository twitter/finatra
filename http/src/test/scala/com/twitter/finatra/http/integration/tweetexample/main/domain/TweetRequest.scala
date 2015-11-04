package com.twitter.finatra.http.integration.tweetexample.main.domain

import com.twitter.finatra.validation.{MethodValidation, Min, ValidationResult}

case class TweetRequest(
  @Min(1) customId: Long,
  username: String,
  tweetMsg: String) {

  @MethodValidation
  def fooCheck = {
    ValidationResult.validate(
      username != "foo",
      "username cannot be foo")
  }
}
