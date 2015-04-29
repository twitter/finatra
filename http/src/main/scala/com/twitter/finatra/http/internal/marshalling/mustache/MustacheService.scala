package com.twitter.finatra.http.internal.marshalling.mustache

import com.github.mustachejava.{Mustache, MustacheFactory}
import com.twitter.finatra.utils.AutoClosable.tryWith
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBufferOutputStream, ChannelBuffers}
import scala.collection.convert.decorateAsScala._

@Singleton
class MustacheService @Inject()(
  mustacheFactory: MustacheFactory) {

  private val templateToMustacheCache =
    new ConcurrentHashMap[String, Mustache]().asScala

  /* Public */

  def createChannelBuffer(templateName: String, obj: Any): ChannelBuffer = {
    val mustache = lookupMustache(templateName)
    tryWith(new ChannelBufferOutputStream(ChannelBuffers.dynamicBuffer(1024))) { cbos =>
      tryWith(new OutputStreamWriter(cbos, StandardCharsets.UTF_8)) { writer =>
        mustache.execute(writer, obj)
        cbos.buffer()
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
