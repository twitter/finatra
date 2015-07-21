package finatra.quickstart.domain.http

object RenderableLocation {
  def create(optLat: Option[Double], optLong: Option[Double]) = {
    for {
      lat <- optLat
      long <- optLong
    } yield RenderableLocation(lat.toString, long.toString)
  }
}

case class RenderableLocation(
  lat: String,
  long: String)
