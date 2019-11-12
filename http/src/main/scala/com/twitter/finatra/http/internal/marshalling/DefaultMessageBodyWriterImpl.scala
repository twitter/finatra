package com.twitter.finatra.http.internal.marshalling

import com.twitter.finagle.http.MediaType
import com.twitter.finatra.http.marshalling.{DefaultMessageBodyWriter, MessageBodyFlags, WriterResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.annotations.Flag
import javax.inject.{Inject, Singleton}

@Singleton
private[finatra] class DefaultMessageBodyWriterImpl @Inject()(
  @Flag(MessageBodyFlags.ResponseCharsetEnabled) includeContentTypeCharset: Boolean,
  mapper: FinatraObjectMapper
) extends DefaultMessageBodyWriter {

  private val jsonCharset = {
    if (includeContentTypeCharset) MediaType.JsonUtf8
    else MediaType.Json
  }

  private val plainText = {
    if (includeContentTypeCharset) MediaType.PlainTextUtf8
    else MediaType.PlainText
  }

  /* Public */

  override def write(obj: Any): WriterResponse = {
    if (isPrimitiveOrWrapper(obj.getClass))
      WriterResponse(plainText, obj.toString)
    else
      WriterResponse(jsonCharset, mapper.writeValueAsBytes(obj))
  }

  /* Private */

  private[this] def isPrimitiveWrapper(clazz: Class[_]): Boolean =
    clazz == classOf[java.lang.Double] ||
      clazz == classOf[java.lang.Float] ||
      clazz == classOf[java.lang.Long] ||
      clazz == classOf[java.lang.Integer] ||
      clazz == classOf[java.lang.Short] ||
      clazz == classOf[java.lang.Character] ||
      clazz == classOf[java.lang.Byte] ||
      clazz == classOf[java.lang.Boolean] ||
      clazz == classOf[java.lang.Void]

  private def isPrimitiveOrWrapper(clazz: Class[_]): Boolean =
    clazz.isPrimitive || isPrimitiveWrapper(clazz)
}
