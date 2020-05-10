package com.twitter.finatra.http.jsonpatch

import com.twitter.finagle.http.Message
import com.twitter.finatra.http.marshalling.mapper._
import com.twitter.finatra.http.marshalling.MessageBodyReader
import com.twitter.finatra.jackson.ScalaObjectMapper
import javax.inject.Inject

/**
 * Transform an HTTP Message to [[com.twitter.finatra.http.jsonpatch JsonPatch]]
 */
class JsonPatchMessageBodyReader @Inject() (mapper: ScalaObjectMapper)
    extends MessageBodyReader[JsonPatch] {

  override def parse(message: Message): JsonPatch = {
    message.contentType match {
      case Some(contentType) if contentType == Message.ContentTypeJsonPatch =>
        val operations = mapper.parseMessageBody[Seq[PatchOperation]](message)
        JsonPatch(operations)
      case _ =>
        throw new JsonPatchException(
          "incorrect Content-Type, should be application/json-patch+json"
        )
    }
  }
}
