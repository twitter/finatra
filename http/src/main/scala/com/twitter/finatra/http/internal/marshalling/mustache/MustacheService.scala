package com.twitter.finatra.http.internal.marshalling.mustache

import com.github.mustachejava.{Mustache, MustacheFactory}
import com.twitter.finatra.utils.AutoClosable.tryWith
import com.twitter.io.Buf
import java.io.{ByteArrayOutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}
import scala.collection.convert.decorateAsScala._

@Singleton
class MustacheService @Inject()(
  mustacheFactory: MustacheFactory) {

  private val templateToMustacheCache =
    new ConcurrentHashMap[String, Mustache]().asScala

  /* Public */

  def createBuffer(templateName: String, obj: Any): Buf = {
    val mustache = lookupMustache(templateName)
    tryWith(new ByteArrayOutputStream(1024)) { os =>
      tryWith(new OutputStreamWriter(os, StandardCharsets.UTF_8)) { writer =>
        mustache.execute(writer, obj)
        writer.close()
        Buf.ByteArray.Owned(os.toByteArray)
      }
    }
  }

  /* Private */

  private def lookupMustache(templateName: String): Mustache = {
    templateToMustacheCache.getOrElseUpdate(templateName, {
      mustacheFactory.compile(templateName)
    })
  }
}
