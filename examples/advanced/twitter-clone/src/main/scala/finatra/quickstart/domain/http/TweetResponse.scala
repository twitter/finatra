package finatra.quickstart.domain.http

import finatra.quickstart.domain.{Tweet, TweetId}

object TweetResponse {
  def fromDomain(tweet: Tweet) = {
    TweetResponse(id = tweet.id, message = tweet.text, location = tweet.location map { location =>
      TweetLocation(location.lat, location.long)
    }, nsfw = tweet.nsfw)
  }
}

case class TweetResponse(
  id: TweetId,
  message: String,
  location: Option[TweetLocation],
  nsfw: Boolean
) {

  def toDomain = {
    Tweet(id, message, location map { _.toDomain }, nsfw)
  }
}
