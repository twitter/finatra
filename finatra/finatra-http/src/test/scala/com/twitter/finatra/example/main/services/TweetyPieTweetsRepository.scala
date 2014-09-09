package com.twitter.finatra.example.main.services

import com.twitter.finatra.example.main.domain.Tweet
import com.twitter.util.Future

class TweetyPieTweetsRepository extends TweetsRepository {

  def getById(id: Long): Future[Option[Tweet]] = {
    Future {
      Some(
        Tweet(
          id = id,
          user = "jack",
          msg = "just setting up my twttr"))
    }
  }
}
