package com.twitter.finatra.twitterserver

trait Handler {
  def handle(): Unit
}
