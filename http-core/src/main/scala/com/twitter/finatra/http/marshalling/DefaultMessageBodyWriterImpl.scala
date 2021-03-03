package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.MediaType
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.utils.{AutoClosable, FileResolver}
import com.twitter.inject.annotations.Flag
import com.twitter.io.{Buf, StreamIO}
import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import scala.runtime.BoxedUnit

private class DefaultMessageBodyWriterImpl(
  @Flag(MessageBodyFlags.ResponseCharsetEnabled) includeContentTypeCharset: Boolean,
  fileResolver: FileResolver,
  mapper: ScalaObjectMapper)
    extends DefaultMessageBodyWriter {

  private[this] val applicationJson =
    if (includeContentTypeCharset) MediaType.JsonUtf8
    else MediaType.Json

  private[this] val plainText =
    if (includeContentTypeCharset) MediaType.PlainTextUtf8
    else MediaType.PlainText

  private[this] val octetStream = MediaType.OctetStream

  /* Public */

  override def write(obj: Any): WriterResponse = {
    obj match {
      case null =>
        WriterResponse.EmptyResponse
      case buf: Buf =>
        WriterResponse(octetStream, buf)
      case bytes: Array[Byte] =>
        WriterResponse(octetStream, bytes)
      case "" =>
        WriterResponse.EmptyResponse
      case () =>
        WriterResponse.EmptyResponse
      case _: BoxedUnit =>
        WriterResponse.EmptyResponse
      case opt if opt == None =>
        WriterResponse.EmptyResponse
      case str: String =>
        WriterResponse(plainText, str)
      case is: InputStream =>
        AutoClosable.tryWith(is) { closable =>
          WriterResponse(octetStream, StreamIO.buffer(closable).toByteArray)
        }
      case file: File =>
        AutoClosable.tryWith(new BufferedInputStream(new FileInputStream(file))) { closable =>
          WriterResponse(
            contentType = fileResolver.getContentType(file.getName),
            StreamIO.buffer(closable).toByteArray)
        }
      case _ =>
        if (isPrimitiveOrWrapper(obj.getClass))
          WriterResponse(plainText, obj.toString)
        else
          WriterResponse(applicationJson, mapper.writeValueAsBytes(obj))
    }
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
