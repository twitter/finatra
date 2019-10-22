package com.twitter.finatra.http.internal.marshalling

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.inject.Injector
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.marshalling.DefaultMessageBodyReader
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.request.JsonIgnoreBody
import javax.inject.{Inject, Singleton}

private[finatra] object DefaultMessageBodyReaderImpl {
  private val EmptyObjectNode = new ObjectNode(null)
}

@Singleton
private[finatra] class DefaultMessageBodyReaderImpl @Inject()(
  injector: Injector,
  objectMapper: FinatraObjectMapper
) extends DefaultMessageBodyReader {

  /* Public */

  override def parse[T: Manifest](request: Request): T = {
    val requestAwareObjectReader = {
      val requestInjectableValues = new RequestInjectableValues(objectMapper, request, injector)
      objectMapper.reader[T].`with`(requestInjectableValues)
    }

    val length = request.contentLength.getOrElse(0L)
    if (length > 0 && isJsonEncoded(request) && !ignoresBody)
      FinatraObjectMapper.parseRequestBody(request, requestAwareObjectReader)
    else
      requestAwareObjectReader.readValue(DefaultMessageBodyReaderImpl.EmptyObjectNode)
  }

  /* Private */

  private def ignoresBody[T: Manifest]: Boolean = {
    manifest[T].runtimeClass.isAnnotationPresent(classOf[JsonIgnoreBody])
  }

  private def isJsonEncoded(request: Request): Boolean = {
    request.contentType.exists { contentType =>
      contentType.startsWith("application/json")
    }
  }
}
