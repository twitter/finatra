package finatra.quickstart.domain.http

import finatra.quickstart.domain.{Status, StatusId}

object RenderableTweet {
  def fromDomain(status: Status) = {
    RenderableTweet(
      id = status.id,
      message = status.text,
      location = RenderableLocation.create(status.lat, status.long),
      sensitive = status.sensitive)
  }
}

case class RenderableTweet(
  id: StatusId,
  message: String,
  location: Option[RenderableLocation],
  sensitive: Boolean)
