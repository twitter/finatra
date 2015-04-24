package com.twitter.finatra.integration.doeverything.main.domain

import com.twitter.finatra.validation.MethodValidation

case class TestUserWithInvalidMethodValidation(
  name: String) {

  @MethodValidation
  def fooCheck = {
    throw new Exception("method validation error")
  }
}