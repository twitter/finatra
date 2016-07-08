package com.twitter.finatra.http.tests.integration.tweetexample.main.services

import com.twitter.concurrent.AsyncStream
import com.twitter.concurrent.AsyncStream.fromOption
import com.twitter.finatra.http.tests.integration.tweetexample.main.domain.Tweet
import com.twitter.util.Future

class MyTweetsRepository extends TweetsRepository {

  private val tweets = Map[Long, Tweet](
    1L -> Tweet(
      id = 1,
      user = "Bob",
      msg = "whats up"),
    2L -> Tweet(
      id = 2,
      user = "Sally",
      msg = "yo"),
    3L -> Tweet(
      id = 3,
      user = "Fred",
      msg = "hey"))

  def getById(id: Long): Future[Option[Tweet]] = {
    Future.value(tweets.get(id))
  }

  def getByIds(ids: AsyncStream[Long]): AsyncStream[Tweet] = {
    ids map tweets.get flatMap fromOption
  }
}
