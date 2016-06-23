package com.twitter.finatra.http.internal.marshalling

import com.google.common.net.MediaType._
import com.twitter.finatra.http.marshalling.{DefaultMessageBodyWriter, WriterResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.Inject
import org.apache.commons.lang.ClassUtils

private[finatra] class FinatraDefaultMessageBodyWriter @Inject()(
  mapper: FinatraObjectMapper)
  extends DefaultMessageBodyWriter {

  /* Public */

  override def write(obj: Any): WriterResponse = {
    if (isPrimitiveOrWrapper(obj.getClass))
      WriterResponse(PLAIN_TEXT_UTF_8, obj.toString)
    else
      WriterResponse(JSON_UTF_8, mapper.writeValueAsBytes(obj))
  }

  /* Private */

  // Note: The following method is included in commons-lang 3.1+
  private def isPrimitiveOrWrapper(clazz: Class[_]): Boolean = {
    clazz.isPrimitive || ClassUtils.wrapperToPrimitive(clazz) != null
  }
}
