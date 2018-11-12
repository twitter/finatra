package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.http.response.Mustache

@Mustache("testuser")
case class TestUserView(age: Int, name: String, friends: Seq[String])
