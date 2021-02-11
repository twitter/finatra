package com.twitter.finatra.http.tests.integration.tweetexample.main.services

import com.twitter.finatra.http.tests.integration.tweetexample.main.domain.Tweet
import com.twitter.io.Reader
import com.twitter.util.Future

class MyTweetsRepository extends TweetsRepository {

  private val tweets = Map[Long, Tweet](
    1L -> Tweet(id = 1, user = "Bob", msg = "whats up"),
    2L -> Tweet(id = 2, user = "Sally", msg = "yo"),
    3L -> Tweet(id = 3, user = "Fred", msg = "hey")
  )

  def getById(id: Long): Future[Option[Tweet]] = {
    Future.value(tweets.get(id))
  }

  def getByIds(ids: Reader[Long]): Reader[Tweet] = {
    ids.map(tweets.get).flatMap {
      case Some(a) => Reader.value(a)
      case None => Reader.empty
    }
  }
}
