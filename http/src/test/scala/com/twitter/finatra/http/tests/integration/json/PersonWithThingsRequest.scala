package com.twitter.finatra.http.tests.integration.json

import com.twitter.finatra.validation.Size

case class PersonWithThingsRequest(
  id: Int,
  name: String,
  age: Option[Int],
  @Size(min = 1, max = 10) things: Map[String, Things])

case class Things(
  @Size(min = 1, max = 2) names: Seq[String])
