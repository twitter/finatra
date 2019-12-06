package com.twitter.finatra.http.tests.integration.tweetexample.main.domain

import com.twitter.finagle.http.Message
import com.twitter.finatra.http.marshalling.mapper._
import com.twitter.finatra.http.marshalling.MessageBodyReader
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.Inject

class TweetMessageBodyReader @Inject()(mapper: FinatraObjectMapper)
    extends MessageBodyReader[Tweet] {

  override def parse(message: Message): Tweet = {
    val tweetRequest = mapper.parseMessageBody[TweetRequest](message)
    Tweet(tweetRequest.customId, tweetRequest.username, tweetRequest.tweetMsg)
  }
}
