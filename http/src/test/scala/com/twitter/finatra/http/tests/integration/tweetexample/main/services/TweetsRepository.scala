package com.twitter.finatra.http.tests.integration.tweetexample.main.services

import com.twitter.finatra.http.tests.integration.tweetexample.main.domain.Tweet
import com.twitter.io.Reader
import com.twitter.util.Future

trait TweetsRepository {

  def getById(id: Long): Future[Option[Tweet]]
  def getByIds(ids: Reader[Long]): Reader[Tweet]
}
