package finatra.quickstart.domain

import com.twitter.finatra.domain.WrappedValue

case class TweetId(
  id: String)
  extends WrappedValue[String]