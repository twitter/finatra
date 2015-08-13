package finatra.quickstart.domain

case class Tweet(
  id: TweetId,
  message: String,
  location: Option[Location],
  nsfw: Boolean)
