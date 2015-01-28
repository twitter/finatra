package com.twitter.finatra.marshalling

import com.google.common.net.MediaType._
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.Inject

class FinatraDefaultMessageBodyWriter @Inject()(
  mapper: FinatraObjectMapper)
  extends DefaultMessageBodyWriter {

  /* Public */

  override def write(obj: Any): WriterResponse = {
    obj match {
      case product: Product =>
        toJson(product)
      case _ if isCollectionType(obj) =>
        toJson(obj)
      case bytes: Array[Byte] =>
        WriterResponse(APPLICATION_BINARY, bytes)
      case _ =>
        WriterResponse(PLAIN_TEXT_UTF_8, obj.toString)
    }
  }

  /* Private */

  private def toJson(obj: Any): WriterResponse = {
    WriterResponse(
      JSON_UTF_8,
      mapper.writeValueAsBytes(obj))
  }

  private def isCollectionType(obj: Any): Boolean = {
    classOf[Iterable[Any]].isAssignableFrom(obj.getClass) ||
      classOf[Array[Any]].isAssignableFrom(obj.getClass)
  }
}
