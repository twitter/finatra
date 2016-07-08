package com.twitter.finatra.http.tests.integration.tweetexample.main.services.admin

import com.twitter.finatra.http.tests.integration.tweetexample.main.services.TweetsRepository
import javax.inject.Inject

class UserService @Inject()(
  database: DatabaseClient,
  tweetsRepository: TweetsRepository) {

  def get(id: String) = {
    database.get(id)
  }
}
