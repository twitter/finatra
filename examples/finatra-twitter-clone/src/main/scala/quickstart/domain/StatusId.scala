package finatra.quickstart.domain

import com.twitter.finatra.domain.WrappedValue

case class StatusId(
  id: String)
  extends WrappedValue[String]