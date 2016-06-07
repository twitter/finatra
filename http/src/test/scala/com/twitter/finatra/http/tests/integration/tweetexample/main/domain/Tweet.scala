package com.twitter.finatra.http.tests.integration.tweetexample.main.domain

case class Tweet(
  id: Long,
  user: String,
  msg: String)
