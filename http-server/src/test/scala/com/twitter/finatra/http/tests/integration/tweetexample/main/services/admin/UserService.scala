package com.twitter.finatra.http.tests.integration.tweetexample.main.services.admin

import com.twitter.finatra.http.tests.integration.tweetexample.main.services.TweetsRepository
import javax.inject.Inject

class UserService @Inject() (database: DatabaseClient, tweetsRepository: TweetsRepository) {
  assert(tweetsRepository != null)

  def get(id: String): String = {
    database.get(id)
  }
}
