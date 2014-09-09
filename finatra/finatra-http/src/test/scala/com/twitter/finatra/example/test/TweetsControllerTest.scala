package com.twitter.finatra.example.test

import com.twitter.finatra.example.main.controllers.TweetsController
import com.twitter.finatra.example.main.domain.Tweet
import com.twitter.finatra.example.main.services.TweetsRepository
import com.twitter.finatra.test.ControllerTest
import com.twitter.util.Future

class TweetsControllerTest extends ControllerTest {

  /* Setup Mocks */
  val tweet20 = Tweet(20, "bob", "hello")
  val tweetsRepository = smartMock[TweetsRepository]
  tweetsRepository.getById(20) returns Future(Some(tweet20))

  /* Create class under test */
  override val controller = new TweetsController(tweetsRepository)

  "get tweet 20" in {
    pending
    val tweet = getAndWait[Tweet]("/tweets/20")
    tweet should equal(tweet20)
  }
}
