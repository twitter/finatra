package com.twitter.finatra.http.marshalling.mustache

import com.github.mustachejava.MustacheFactory
import com.twitter.io.Buf
import java.io.{ByteArrayOutputStream, OutputStreamWriter, StringWriter}
import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}

/**
 * A service for rendering mustache templates. Note: it is expected to be rare
 * when you would need to interact directly with this service. The framework
 * will handle rendering and returning mustache templates over a view object.
 *
 * A use-case where you may need this service is if you wanted to use a template
 * to generate content to be embedded inside of a response instead of being
 * the entire response. For this we have provided the #createString method.
 * @param mustacheFactory the factory to use for compiling a given template
 */
@Singleton
class MustacheService @Inject()(
  mustacheFactory: MustacheFactory) {

  private[finatra] def createBuffer(templateName: String, obj: Any): Buf = {
    val mustache = mustacheFactory.compile(templateName)

    val outputStream = new ByteArrayOutputStream(1024)
    val writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
    try {
      mustache.execute(writer, obj)
    } finally {
      writer.close()
    }

    Buf.ByteArray.Owned(outputStream.toByteArray)
  }

  def createString(templateName: String, obj: Any): String = {
    val mustache = mustacheFactory.compile(templateName)

    val writer = new StringWriter()
    mustache.execute(writer, obj).flush()

    writer.toString
  }
}
