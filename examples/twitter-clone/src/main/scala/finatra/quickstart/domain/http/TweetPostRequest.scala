package finatra.quickstart.domain.http

import com.twitter.finatra.validation.Size
import finatra.quickstart.domain.{Tweet, TweetId}

case class TweetPostRequest(
  @Size(min = 1, max = 140) message: String,
  location: Option[TweetLocation],
  nsfw: Boolean = false
) {

  def toDomain(id: TweetId) = {
    Tweet(id = id, text = message, location = location map { _.toDomain }, nsfw = nsfw)
  }
}
