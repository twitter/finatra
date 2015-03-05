package com.twitter.finatra.integration.tweetexample.main.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.Controller
import com.twitter.finatra.integration.tweetexample.main.domain.Tweet
import com.twitter.finatra.integration.tweetexample.main.services.TweetsRepository
import javax.inject.Inject

class TweetsController @Inject()(
  tweetsRepository: TweetsRepository)
  extends Controller {

  get("/tweets/hello") { request: Request =>
    "hello world"
  }

  get("/tweets/:id") { request: Request =>
    val id = request.params("id").toLong
    tweetsRepository.getById(id)
  }

  post("/tweets/") { tweet: Tweet =>
    "tweet with id " + tweet.id + " is valid"
  }
}
