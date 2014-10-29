package com.twitter.finatra.marshalling

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.inject.Injector
import com.twitter.finagle.http.Request
import com.twitter.finatra.conversions.strings._
import com.twitter.finatra.exceptions.BadRequestException
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.request.RequestInjectableValues
import javax.inject.{Inject, Singleton}

object JsonMessageBodyReader {
  private val EmptyObjectNode = new ObjectNode(null)
}

@Singleton
class JsonMessageBodyReader @Inject()(
  injector: Injector,
  objectMapper: FinatraObjectMapper)
  extends DefaultMessageBodyReader {

  override def parse[T: Manifest](request: Request): T = {
    val requestInjectableValues = new RequestInjectableValues(request, injector)
    val requestAwareObjectReader = objectMapper.reader[T].`with`(requestInjectableValues)

    val length = request.contentLength.getOrElse(0L)
    if (length == 0 ||
      (length > 0 && isFormEncoded(request)))
      requestAwareObjectReader.readValue(JsonMessageBodyReader.EmptyObjectNode)
    else if (isJsonEncoded(request))
      FinatraObjectMapper.parseRequestBody(request, requestAwareObjectReader)
    else
      throw new BadRequestException("Can't parse request body with content-type: " + request.contentType.getOrElse("unknown") + " and body: " + request.contentString.ellipse(50))
  }

  /* Private */

  private def isFormEncoded(request: Request): Boolean = {
    request.contentType.exists { contentType =>
      contentType.startsWith("application/x-www-form-urlencoded") ||
        contentType.startsWith("multipart/")
    }
  }

  private def isJsonEncoded(request: Request): Boolean = {
    request.contentType.exists { contentType =>
      contentType.startsWith("application/json")
    }
  }
}
