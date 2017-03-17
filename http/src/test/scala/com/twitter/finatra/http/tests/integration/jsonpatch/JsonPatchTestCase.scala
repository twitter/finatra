package com.twitter.http.tests.integration.jsonpatch

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.http.jsonpatch.PatchOperation

case class JsonPatchTestCase(
  comment: Option[String],
  error: Option[String],
  doc: JsonNode,
  patch: Seq[PatchOperation],
  expected: Option[JsonNode]
)
