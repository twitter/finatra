package com.twitter.finatra.integration.tweetexample.main.domain

import com.google.common.net.MediaType
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.marshalling.{MessageBodyWriter, WriterResponse}
import javax.inject.Inject

class TweetMessageBodyWriter @Inject()(
  mapper: FinatraObjectMapper)
  extends MessageBodyWriter[Tweet] {

  override def write(tweet: Tweet) = {
    WriterResponse(
      MediaType.JSON_UTF_8,
      mapper.writeValueAsBytes(Map(
        "idonly" -> tweet.id)))
  }
}