package com.twitter.finatra.http.internal.marshalling

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.inject.Injector
import com.twitter.finagle.http.{MediaType, Message, Request}
import com.twitter.finatra.http.marshalling.{DefaultMessageBodyReader, MessageBodyReader}
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

  override def parse[T: Manifest](message: Message): T = {
    val objectReader = message match {
      case request: Request =>
        val requestInjectableValues =
          new RequestInjectableValues(objectMapper,request, injector)
        objectMapper.reader[T].`with`(requestInjectableValues)
      case _ =>
        objectMapper.reader[T]
    }

    val hasMessageBody = message.contentLength match {
      case Some(length) if length > 0 => true
      case _ => false
    }

    if (hasMessageBody && !ignoresBody[T] && isBodyJsonEncoded(message)) {
      // the body of the message should be parsed by the object reader
      MessageBodyReader.parseMessageBody[T](message, objectReader)
    } else {
      // use the object reader simply to trigger the framework
      // case class deserializer over an empty object node
      objectReader.readValue[T](DefaultMessageBodyReaderImpl.EmptyObjectNode)
    }
  }

  /* Private */

  private def ignoresBody[T: Manifest]: Boolean = {
    manifest[T].runtimeClass.isAnnotationPresent(classOf[JsonIgnoreBody])
  }

  private def isBodyJsonEncoded(message: Message): Boolean =
    message.contentType.exists(_.startsWith(MediaType.Json))
}
