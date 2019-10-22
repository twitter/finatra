package com.twitter.finatra.http.jsonpatch

import com.twitter.finagle.http.{Message, Request}
import com.twitter.finatra.http.marshalling.MessageBodyReader
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.Inject

/**
 * Transform HTTP Request to [[com.twitter.finatra.http.jsonpatch JsonPatch]]
 */
class JsonPatchMessageBodyReader @Inject()(mapper: FinatraObjectMapper)
    extends MessageBodyReader[JsonPatch] {

  override def parse[M: Manifest](request: Request): JsonPatch = {
    request.contentType match {
      case Some(contentType) if contentType == Message.ContentTypeJsonPatch =>
        val operations = mapper.parse[Seq[PatchOperation]](request)
        JsonPatch(operations)
      case _ =>
        throw new JsonPatchException(
          "incorrect Content-Type, should be application/json-patch+json"
        )
    }
  }
}
