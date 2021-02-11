package com.twitter.finatra.http.tests.integration.tweetexample.main.domain

import com.twitter.finagle.http.MediaType
import com.twitter.finatra.http.marshalling.{MessageBodyWriter, WriterResponse}
import com.twitter.finatra.jackson.ScalaObjectMapper
import javax.inject.Inject

class TweetMessageBodyWriter @Inject() (mapper: ScalaObjectMapper)
    extends MessageBodyWriter[Tweet] {

  override def write(tweet: Tweet): WriterResponse = {
    WriterResponse(MediaType.JsonUtf8, mapper.writeValueAsBytes(Map("idonly" -> tweet.id)))
  }
}
