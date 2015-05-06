package finatra.quickstart.domain.http

import com.twitter.finatra.validation.Size
import finatra.quickstart.domain.{Status, StatusId}

case class PostedTweet(
  @Size(min = 1, max = 140) message: String,
  location: Option[Location],
  sensitive: Boolean = false) {

  def toDomain(id: StatusId) = {
    Status(
      id = id,
      text = message,
      lat = location map {_.lat},
      long = location map {_.long},
      sensitive = sensitive)
  }
}
