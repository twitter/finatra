package com.twitter.finatra.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import com.fasterxml.jackson.databind.{JsonNode, Module, ObjectMapper, ObjectReader}
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.twitter.finagle.http.Request
import com.twitter.finatra.json.internal.caseclass.exceptions.RequestFieldInjectionNotSupportedException
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
    objectMapper.convertValue[T](jsonNode)
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

  def convert[T: Manifest](any: Any): T = {
    objectMapper.convertValue[T](any)
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
