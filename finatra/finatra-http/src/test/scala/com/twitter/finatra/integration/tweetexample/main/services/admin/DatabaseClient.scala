package com.twitter.finatra.integration.tweetexample.main.services.admin

class DatabaseClient(
  url: String) {

  def get(id: String) = {
    s"$id from $url"
  }
}
