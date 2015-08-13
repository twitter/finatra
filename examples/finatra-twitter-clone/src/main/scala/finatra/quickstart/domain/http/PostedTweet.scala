package finatra.quickstart.domain.http

import com.twitter.finatra.validation.Size
import finatra.quickstart.domain.{Tweet, TweetId}

case class PostedTweet(
  @Size(min = 1, max = 140) message: String,
  location: Option[PostedLocation],
  nsfw: Boolean = false) {

  def toDomain(id: TweetId) = {
    Tweet(
      id = id,
      message = message,
      location = location map {_.toDomain},
      nsfw = nsfw)
  }
}
