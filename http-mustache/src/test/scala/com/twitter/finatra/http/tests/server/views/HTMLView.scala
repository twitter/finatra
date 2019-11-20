package com.twitter.finatra.http.tests.server.views

import com.twitter.finatra.http.annotations.Mustache

@Mustache("testHtml")
case class HTMLView(age: Int, name: String, friends: Seq[String])
