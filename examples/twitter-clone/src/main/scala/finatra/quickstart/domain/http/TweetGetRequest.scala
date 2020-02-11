package finatra.quickstart.domain.http

import com.twitter.finatra.http.annotations.RouteParam
import finatra.quickstart.domain.TweetId

case class TweetGetRequest(@RouteParam id: TweetId)
