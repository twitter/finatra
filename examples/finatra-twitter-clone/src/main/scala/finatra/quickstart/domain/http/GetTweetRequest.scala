package finatra.quickstart.domain.http

import com.twitter.finatra.request.RouteParam
import finatra.quickstart.domain.TweetId

case class GetTweetRequest(
  @RouteParam id: TweetId)