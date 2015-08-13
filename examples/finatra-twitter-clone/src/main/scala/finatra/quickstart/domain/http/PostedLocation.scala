package finatra.quickstart.domain.http

import com.twitter.finatra.validation._
import finatra.quickstart.domain.Location

case class PostedLocation(
  @Range(min = -85, max = 85) lat: Double,
  @Range(min = -180, max = 180) long: Double) {

  def toDomain = {
    Location(lat, long)
  }
}
