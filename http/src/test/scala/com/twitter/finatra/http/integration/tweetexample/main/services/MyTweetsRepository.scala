package com.twitter.finatra.http.integration.tweetexample.main.services

import com.twitter.finatra.http.integration.tweetexample.main.domain.Tweet
import com.twitter.util.Future

class MyTweetsRepository extends TweetsRepository {

  def getById(id: Long): Future[Option[Tweet]] = {
    Future {
      Some(
        Tweet(
          id = id,
          user = "Bob",
          msg = "whats up"))
    }
  }
}
