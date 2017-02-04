package finatra.quickstart.domain

import com.twitter.inject.domain.WrappedValue

case class TweetId(
  id: String)
  extends WrappedValue[String]