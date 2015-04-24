package com.twitter.finatra.integration.doeverything.main.domain

import com.twitter.finatra.response.Mustache

@Mustache("testuser")
case class TestUserView(
   age: Int,
   name: String,
   friends: Seq[String])
