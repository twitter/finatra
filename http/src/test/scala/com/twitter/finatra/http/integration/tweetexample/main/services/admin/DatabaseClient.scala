package com.twitter.finatra.http.integration.tweetexample.main.services.admin

class DatabaseClient(
  url: String) {

  def get(id: String) = {
    s"$id from $url"
  }
}
