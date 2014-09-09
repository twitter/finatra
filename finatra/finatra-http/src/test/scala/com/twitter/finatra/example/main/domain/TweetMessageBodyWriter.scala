package com.twitter.finatra.example.main.domain

import com.google.common.net.MediaType
import com.twitter.finatra.conversions.json._
import com.twitter.finatra.marshalling.{MessageBodyWriter, WriterResponse}

class TweetMessageBodyWriter extends MessageBodyWriter[Tweet] {

  override def write(tweet: Tweet) = {
    WriterResponse(
      MediaType.JSON_UTF_8,
      Map("idonly" -> tweet.id).toJsonBytes)
  }
}