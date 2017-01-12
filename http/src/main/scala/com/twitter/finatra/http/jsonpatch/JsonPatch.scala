package com.twitter.finatra.http.jsonpatch

/**
 * JsonPatch represents a sequence of operations to apply to a JSON document.
 * The corresponding HTTP request should use application/json-patch+json as the Content-Type.
 * @see [[com.twitter.finagle.http.MediaType application/json-patch+json]]
 *
 * @param patches a Seq of Json Patch Operations
 * @see [[https://tools.ietf.org/html/rfc69012 RFC 6902]]
 */
case class JsonPatch(patches: Seq[PatchOperation])
