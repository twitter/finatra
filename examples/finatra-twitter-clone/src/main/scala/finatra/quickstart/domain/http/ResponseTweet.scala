package finatra.quickstart.domain.http

import finatra.quickstart.domain.{Tweet, TweetId}

object ResponseTweet {
  def fromDomain(tweet: Tweet) = {
    ResponseTweet(
      id = tweet.id,
      message = tweet.message,
      location = tweet.location map { location =>
        PostedLocation(location.lat, location.long)
      },
      nsfw = tweet.nsfw)
  }
}

case class ResponseTweet(
  id: TweetId,
  message: String,
  location: Option[PostedLocation],
  nsfw: Boolean) {

  def toDomain = {
    Tweet(
      id,
      message,
      location map { _.toDomain },
      nsfw)
  }
}
