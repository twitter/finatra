package com.twitter.finatra.http.internal.marshalling.mustache

import com.google.common.net.MediaType
import com.twitter.finatra.http.marshalling.{MessageBodyWriter, WriterResponse}
import com.twitter.finatra.response.Mustache
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}
import scala.collection.JavaConversions.mapAsScalaConcurrentMap

@Singleton
class MustacheMessageBodyWriter @Inject()(
  mustacheService: MustacheService)
  extends MessageBodyWriter[Any] {

  private val classToViewNameCache = mapAsScalaConcurrentMap(
    new ConcurrentHashMap[Class[_], String]())

  /* Public */

  override def write(obj: Any): WriterResponse = {
    WriterResponse(
      MediaType.HTML_UTF_8,
      mustacheService.createChannelBuffer(
        lookupTemplateName(obj),
        obj))
  }

  /* Private */


  private def lookupTemplateName(viewObj: Any): String = {
    classToViewNameCache.getOrElseUpdate(viewObj.getClass, {
      val mustacheAnnotation = viewObj.getClass.getAnnotation(classOf[Mustache])
      mustacheAnnotation.value + ".mustache"
    })
  }
}
