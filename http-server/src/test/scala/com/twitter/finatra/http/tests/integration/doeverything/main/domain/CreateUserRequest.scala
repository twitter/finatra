package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.http.annotations.Header

case class CreateUserRequest(@Header requestId: String, name: String, age: Int)
