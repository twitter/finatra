package com.twitter.finatra.integration.tweetexample.main.services.admin

import com.twitter.finatra.integration.tweetexample.main.services.TweetsRepository
import javax.inject.Inject

class UserService @Inject()(
  database: DatabaseClient,
  tweetsRepository: TweetsRepository) {

  def get(id: String) = {
    database.get(id)
  }
}
