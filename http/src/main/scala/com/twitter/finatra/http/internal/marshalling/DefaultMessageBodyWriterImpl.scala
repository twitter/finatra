package com.twitter.finatra.http.internal.marshalling

import com.twitter.finagle.http.MediaType
import com.twitter.finatra.http.marshalling.{DefaultMessageBodyWriter, WriterResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.annotations.Flag
import javax.inject.Inject
import org.apache.commons.lang.ClassUtils

private[finatra] class DefaultMessageBodyWriterImpl @Inject()(
  @Flag("http.response.charset.enabled") includeContentTypeCharset: Boolean,
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

  // Note: The following method is included in commons-lang 3.1+
  private def isPrimitiveOrWrapper(clazz: Class[_]): Boolean = {
    clazz.isPrimitive || ClassUtils.wrapperToPrimitive(clazz) != null
  }
}
