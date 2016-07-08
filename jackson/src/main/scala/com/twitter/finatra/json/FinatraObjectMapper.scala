package com.twitter.finatra.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.inject.Injector
import com.twitter.finagle.http.{Message, Request, Response}
import com.twitter.finatra.json.internal.caseclass.exceptions.RequestFieldInjectionNotSupportedException
import com.twitter.finatra.json.internal.serde.ArrayElementsOnNewLinesPrettyPrinter
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.io.Buf
import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer

object FinatraObjectMapper {

  /**
   * When not using Guice, this factory method can be used, but be aware that the @JsonInject annotation will not work.
   * NOTE: The preferred way of obtaining a FinatraObjectMapper is through Guice injection using FinatraJacksonModule.
   */
  def create(injector: Injector = null): FinatraObjectMapper = {
    val guiceJacksonModule = new FinatraJacksonModule()
    new FinatraObjectMapper(
      guiceJacksonModule.provideScalaObjectMapper(injector))
  }

  def parseMessageBody[T: Manifest](message: Message, reader: ObjectReader): T = {
    val inputStream = message.getInputStream()
    try {
      reader.readValue[T](inputStream)
    }
    finally {
      inputStream.close()
    }
  }

  def parseRequestBody[T: Manifest](request: Request, reader: ObjectReader): T =
    parseMessageBody[T](request, reader)

  def parseResponseBody[T: Manifest](response: Response, reader: ObjectReader): T =
    parseMessageBody[T](response, reader)
}

case class FinatraObjectMapper(
  objectMapper: ObjectMapper with ScalaObjectMapper) {

  lazy val prettyObjectMapper = {
    objectMapper.writer(ArrayElementsOnNewLinesPrettyPrinter)
  }

  def reader[T: Manifest] = {
    objectMapper.readerFor[T]
  }

  def parse[T: Manifest](message: Message): T = {
    if (message.isRequest) {
      val length = message.contentLength.getOrElse(0L)
      if (length == 0) {
        throw new RequestFieldInjectionNotSupportedException()
      }
    }
    FinatraObjectMapper.parseMessageBody(message, objectMapper.readerFor[T])
  }

  def parse[T: Manifest](byteBuffer: ByteBuffer): T = {
    val is = new ByteBufferBackedInputStream(byteBuffer)
    objectMapper.readValue[T](is)
  }

  def parse[T: Manifest](buf: Buf): T = {
    parse[T](
      Buf.ByteBuffer.Shared.extract(buf))
  }

  def parse[T: Manifest](jsonNode: JsonNode): T = {
    convert[T](jsonNode)
  }

  /** Parse InputStream (caller must close) */
  def parse[T: Manifest](inputStream: InputStream): T = {
    objectMapper.readValue[T](inputStream)
  }

  def parse[T: Manifest](bytes: Array[Byte]): T = {
    objectMapper.readValue[T](bytes)
  }

  def parse[T: Manifest](string: String): T = {
    objectMapper.readValue[T](string)
  }

  def parse[T: Manifest](jsonParser: JsonParser): T = {
    objectMapper.readValue[T](jsonParser)
  }

  /*
   When Finatra's CaseClassMappingException is thrown inside 'convertValue', newer versions of Jackson wrap
   CaseClassMappingException inside an IllegalArgumentException. As such, we unwrap to restore the original exception here.

   Details:
   See https://github.com/FasterXML/jackson-databind/blob/2.4/src/main/java/com/fasterxml/jackson/databind/ObjectMapper.java#L2773
   The wrapping occurs because CaseClassMappingException is an IOException (because we extend JsonMappingException which extends JsonProcessingException which extends IOException).
   We must extend JsonMappingException otherwise CaseClassMappingException is not properly handled when deserializing into nested case-classes
  */
  def convert[T: Manifest](any: Any): T = {
    try {
      objectMapper.convertValue[T](any)
    } catch {
      case e: IllegalArgumentException =>
        throw e.getCause
    }
  }

  /* See scaladoc from convert above */
  def convert(from: Any, toValueType: JavaType): AnyRef = {
    try {
      objectMapper.convertValue(from, toValueType)
    } catch {
      case e: IllegalArgumentException =>
        throw e.getCause
    }
  }

  def writeValueAsBytes(any: Any): Array[Byte] = {
    objectMapper.writeValueAsBytes(any)
  }

  def writeValue(any: Any, outputStream: OutputStream) {
    objectMapper.writeValue(outputStream, any)
  }

  def writeValueAsString(any: Any): String = {
    objectMapper.writeValueAsString(any)
  }

  def writePrettyString(any: Any): String = any match {
    case str: String =>
      val jsonNode = objectMapper.readValue[JsonNode](str)
      prettyObjectMapper.writeValueAsString(jsonNode)
    case _ =>
      prettyObjectMapper.writeValueAsString(any)
  }

  def writeValueAsBuf(any: Any): Buf = {
    Buf.ByteArray.Shared(
      objectMapper.writeValueAsBytes(any))
  }

  def registerModule(module: Module) = {
    objectMapper.registerModule(module)
  }
}
