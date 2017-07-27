package finatra.quickstart.domain

case class Tweet(id: TweetId, text: String, location: Option[Location], nsfw: Boolean)
