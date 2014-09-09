package com.twitter.finatra.benchmarks.domain

case class TestImpressionTaskRequest(
  request_id: String,
  group_ids: Seq[String],
  params: TestImpressionRequestParams)

case class TestImpressionRequestParams(
  results: TestImpressionRequestResults,
  start_time: String, //DateTime,
  end_time: String, //DateTime,
  priority: String)

case class TestImpressionRequestResults(
  country_codes: Array[String],
  group_by: Seq[String],
  format: Option[TestFormat],
  demographics: Seq[TestDemographic])

/* Not supported in scala-jackson-module
case class CountryCode(
  id: String)
  extends JsonWrappedValue
*/