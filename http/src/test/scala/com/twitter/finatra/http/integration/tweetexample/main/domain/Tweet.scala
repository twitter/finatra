package com.twitter.finatra.http.integration.tweetexample.main.domain

case class Tweet(
  id: Long,
  user: String,
  msg: String)
