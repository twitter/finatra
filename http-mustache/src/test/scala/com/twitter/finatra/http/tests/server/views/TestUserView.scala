package com.twitter.finatra.http.tests.server.views

import com.twitter.finatra.http.annotations.Mustache

@Mustache("testuser")
case class TestUserView(age: Int, name: String, friends: Seq[String])
