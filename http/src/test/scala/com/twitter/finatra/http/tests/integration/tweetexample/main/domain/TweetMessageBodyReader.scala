package com.twitter.finatra.http.tests.integration.tweetexample.main.domain

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.marshalling.MessageBodyReader
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.Inject

class TweetMessageBodyReader @Inject()(
  mapper: FinatraObjectMapper)
  extends MessageBodyReader[Tweet] {

  override def parse(request: Request): Tweet = {
    val tweetRequest = mapper.parse[TweetRequest](request)
    Tweet(
      tweetRequest.customId,
      tweetRequest.username,
      tweetRequest.tweetMsg)
  }
}

