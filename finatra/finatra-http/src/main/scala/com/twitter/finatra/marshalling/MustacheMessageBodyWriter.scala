package com.twitter.finatra.marshalling

import com.google.common.net.MediaType
import com.twitter.finatra.annotations.{Mustache => MustacheAnnotation}
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}
import scala.collection.JavaConversions.asScalaConcurrentMap

@Singleton
class MustacheMessageBodyWriter @Inject()(
  mustacheService: MustacheService)
  extends MessageBodyWriter[Any] {

  private val classToViewNameCache =
    asScalaConcurrentMap(new ConcurrentHashMap[Class[_], String]())

  /* Public */

  override def write(obj: Any): WriterResponse = {
    WriterResponse(
      MediaType.HTML_UTF_8,
      mustacheService.writeBytes(
        lookupTemplateName(obj),
        obj))
  }

  /* Private */


  private def lookupTemplateName(viewObj: Any): String = {
    classToViewNameCache.getOrElseUpdate(viewObj.getClass, {
      val mustacheAnnotation = viewObj.getClass.getAnnotation(classOf[MustacheAnnotation])
      mustacheAnnotation.value + ".mustache"
    })
  }
}