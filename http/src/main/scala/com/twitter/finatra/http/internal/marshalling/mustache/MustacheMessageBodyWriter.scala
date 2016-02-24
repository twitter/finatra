package com.twitter.finatra.http.internal.marshalling.mustache

import com.google.common.net.MediaType
import com.twitter.finatra.http.marshalling.mustache.{MustacheBodyComponent, MustacheService}
import com.twitter.finatra.http.marshalling.{MessageBodyWriter, WriterResponse}
import com.twitter.finatra.response.Mustache
import com.twitter.inject.conversions.map._
import com.twitter.io.Buf
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}
import scala.collection.JavaConverters._

@Singleton
class MustacheMessageBodyWriter @Inject()(
  mustacheService: MustacheService)
  extends MessageBodyWriter[Any] {

  private val classToViewNameCache = new ConcurrentHashMap[Class[_], String]().asScala

  /* Public */

  override def write(obj: Any): WriterResponse = {
    WriterResponse(
      MediaType.HTML_UTF_8,
      obj match {
        case c: MustacheBodyComponent => createBuffer(c)
        case _ => createBuffer(obj)
      }
    )
  }

  /* Private */

  private def createBuffer(mustacheBodyComponent: MustacheBodyComponent): Buf = {
    val template = if (mustacheBodyComponent.template.isEmpty) {
      lookupTemplateName(mustacheBodyComponent.data)
    } else {
      mustacheBodyComponent.template
    }
    mustacheService.createBuffer(template, mustacheBodyComponent.data)
  }

  private def createBuffer(obj: Any): Buf = {
    mustacheService.createBuffer(
      lookupTemplateName(obj),
      obj
    )
  }

  private def lookupTemplateName(viewObj: Any): String = {
    classToViewNameCache.atomicGetOrElseUpdate(viewObj.getClass, {
      val mustacheAnnotation = viewObj.getClass.getAnnotation(classOf[Mustache])
      mustacheAnnotation.value + ".mustache"
    })
  }
}
