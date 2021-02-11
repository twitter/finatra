package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.validation.constraints.PastTime

case class TestUserWithInvalidFieldValidation(@PastTime name: String)
