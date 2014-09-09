package com.twitter.finatra.marshalling

import com.github.mustachejava.{Mustache, MustacheFactory}
import com.twitter.finatra.annotations.{Mustache => MustacheAnnotation}
import java.io.{ByteArrayOutputStream, OutputStreamWriter}
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}
import scala.collection.JavaConversions.asScalaConcurrentMap

@Singleton
class MustacheService @Inject()(
  mustacheFactory: MustacheFactory) {

  private val templateToMustacheCache =
    asScalaConcurrentMap(new ConcurrentHashMap[String, Mustache]())

  /* Public */

  //TODO: Optimize
  def writeBytes(templateName: String, obj: Any): Array[Byte] = {
    val mustache = lookupMustache(templateName)
    val baos = new ByteArrayOutputStream()
    val writer = new OutputStreamWriter(baos, "UTF-8")
    mustache.execute(writer, obj)
    writer.close()
    baos.toByteArray
  }

  /* Private */

  private def lookupMustache(templateName: String): Mustache = {
    templateToMustacheCache.getOrElseUpdate(templateName, {
      mustacheFactory.compile(templateName)
    })
  }
}
