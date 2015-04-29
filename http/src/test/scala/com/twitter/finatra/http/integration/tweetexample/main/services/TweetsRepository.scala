package com.twitter.finatra.http.integration.tweetexample.main.services

import com.twitter.finatra.http.integration.tweetexample.main.domain.Tweet
import com.twitter.util.Future

trait TweetsRepository {

  def getById(id: Long): Future[Option[Tweet]]
}
