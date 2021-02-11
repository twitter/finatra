package com.twitter.finatra.http.jsonpatch

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode

/**
 * Operations compose JSON Patch, apply to a target JSON document
 * @see [[https://tools.ietf.org/html/rfc6902 RFC 6902]]
 */
case class PatchOperation(
  op: Operand,
  path: JsonPointer,
  value: Option[JsonNode],
  from: Option[JsonPointer])
