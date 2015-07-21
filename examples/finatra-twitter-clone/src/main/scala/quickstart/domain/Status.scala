package finatra.quickstart.domain

case class Status(
  id: StatusId,
  text: String,
  lat: Option[Double],
  long: Option[Double],
  sensitive: Boolean)

