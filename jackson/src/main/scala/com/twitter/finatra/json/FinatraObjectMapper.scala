package com.twitter.finatra.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import com.fasterxml.jackson.databind.{JsonNode, Module, ObjectMapper, ObjectReader}
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.twitter.finagle.http.Request
import com.twitter.finatra.json.internal.caseclass.exceptions.{JsonObjectParseException, RequestFieldInjectionNotSupportedException}
import com.twitter.finatra.json.modules.FinatraJacksonModule
import java.io.{InputStream, OutputStream, StringWriter}
import java.nio.ByteBuffer

object FinatraObjectMapper {

  /**
   * If Guice is not available, this factory method can be used, but be aware that the @JsonInject annotation will not work.
   * NOTE: The preferred way of obtaining a FinatraObjectMapper is through Guice injection using FinatraJacksonModule.
   */
  def create() = {
    val guiceJacksonModule = new FinatraJacksonModule()
    new FinatraObjectMapper(
      guiceJacksonModule.provideScalaObjectMapper(injector = null))
  }

  def parseRequestBody[T: Manifest](request: Request, reader: ObjectReader): T = {
    val inputStream = request.getInputStream()
    try {
      reader.readValue[T](inputStream)
    }
    finally {
      inputStream.close()
    }
  }
}

case class FinatraObjectMapper(
  objectMapper: ObjectMapper with ScalaObjectMapper) {

  def reader[T: Manifest] = {
    objectMapper.reader[T]
  }

  def parse[T: Manifest](request: Request): T = {
    val length = request.contentLength.getOrElse(0L)
    if (length == 0)
      throw new RequestFieldInjectionNotSupportedException()
    else
      FinatraObjectMapper.parseRequestBody(request, objectMapper.reader[T])
  }

  def parse[T: Manifest](byteBuffer: ByteBuffer): T = {
    val is = new ByteBufferBackedInputStream(byteBuffer)
    objectMapper.readValue[T](is)
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
   When Finatra's JsonObjectParseException is thrown inside 'convertValue', newer versions of Jackson wrap
   JsonObjectParseException inside an IllegalArgumentException. As such, we unwrap to restore the original exception here.

   Details:
   See https://github.com/FasterXML/jackson-databind/blob/2.4/src/main/java/com/fasterxml/jackson/databind/ObjectMapper.java#L2773
   The wrapping occurs because JsonObjectParseException is an IOException (because we extend JsonMappingException which extends JsonProcessingException which extends IOException).
   We must extend JsonMappingException otherwise JsonObjectParseException is not properly handled when deserializing into nested case-classes
  */
  def convert[T: Manifest](any: Any): T = {
    try {
      objectMapper.convertValue[T](any)
    } catch {
      case e: IllegalArgumentException if e.getCause.isInstanceOf[JsonObjectParseException] =>
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

  def writePrettyString(any: Any): String = {
    val writer = new StringWriter()
    val generator = objectMapper.getFactory.createGenerator(writer)
    generator.useDefaultPrettyPrinter()
    generator.writeObject(any)
    writer.toString
  }

  def registerModule(module: Module) = {
    objectMapper.registerModule(module)
  }
}
