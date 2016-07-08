package com.twitter.finatra.http.tests.integration.tweetexample.main.services.admin

class DatabaseClient(
  url: String) {

  def get(id: String) = {
    s"$id from $url"
  }
}
