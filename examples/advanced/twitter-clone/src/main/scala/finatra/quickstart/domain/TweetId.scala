package finatra.quickstart.domain

import com.twitter.util.WrappedValue

case class TweetId(id: String) extends WrappedValue[String]
