package com.twitter.finatra.example.main.services

import com.twitter.finatra.example.main.domain.Tweet
import com.twitter.util.Future

trait TweetsRepository {

  def getById(id: Long): Future[Option[Tweet]]
}
