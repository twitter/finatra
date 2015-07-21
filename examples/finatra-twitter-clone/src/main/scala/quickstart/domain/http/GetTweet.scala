package finatra.quickstart.domain.http

import com.twitter.finatra.request.RouteParam
import finatra.quickstart.domain.StatusId

case class GetTweet(
  @RouteParam id: StatusId)