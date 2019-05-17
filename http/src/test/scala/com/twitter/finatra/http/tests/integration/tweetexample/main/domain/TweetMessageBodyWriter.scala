package com.twitter.finatra.http.tests.integration.tweetexample.main.domain

import com.twitter.finagle.http.MediaType
import com.twitter.finatra.http.marshalling.{MessageBodyWriter, WriterResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.Inject

class TweetMessageBodyWriter @Inject()(mapper: FinatraObjectMapper)
    extends MessageBodyWriter[Tweet] {

  override def write(tweet: Tweet) = {
    WriterResponse(MediaType.JsonUtf8, mapper.writeValueAsBytes(Map("idonly" -> tweet.id)))
  }
}
