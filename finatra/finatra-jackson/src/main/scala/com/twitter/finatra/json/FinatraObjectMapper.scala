package com.twitter.finatra.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{JsonNode, Module, ObjectMapper, ObjectReader}
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.twitter.finagle.http.Request
import com.twitter.finatra.json.internal.caseclass.exceptions.RequestFieldInjectionNotSupportedException
import com.twitter.finatra.json.internal.caseclass.utils.ByteBufferBackedInputStream
import com.twitter.finatra.json.internal.{JsonArrayIterator, JsonStreamParseResult, ReaderIterator}
import com.twitter.finatra.json.modules.FinatraJacksonModule
import java.io.{InputStream, OutputStream, Reader, StringWriter}
import java.nio.ByteBuffer

object FinatraObjectMapper {

  /**
   * This factory method is not recommended in production, but it may be useful in tests.
   * The recommended way of obtaining an FinatraObjectMapper is through Guice injection.
   * Note: The @JsonInject annotation is not supported when this method of creation is used.
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
  mapper: ObjectMapper with ScalaObjectMapper) {

  def reader[T: Manifest] = {
    mapper.reader[T]
  }

  def parse[T: Manifest](request: Request): T = {
    val length = request.contentLength.getOrElse(0L)
    if (length == 0)
      throw new RequestFieldInjectionNotSupportedException()
    else
      FinatraObjectMapper.parseRequestBody(request, mapper.reader[T])
  }

  def parse[T: Manifest](byteBuffer: ByteBuffer): T = {
    val is = new ByteBufferBackedInputStream(byteBuffer)
    mapper.readValue[T](is)
  }

  def parse[T: Manifest](jsonNode: JsonNode): T = {
    mapper.convertValue[T](jsonNode)
  }

  /** Parse InputStream (caller must close) */
  def parse[T: Manifest](inputStream: InputStream): T = {
    mapper.readValue[T](inputStream)
  }

  def parse[T: Manifest](bytes: Array[Byte]): T = {
    mapper.readValue[T](bytes)
  }

  def parse[T: Manifest](string: String): T = {
    mapper.readValue[T](string)
  }

  def parse[T: Manifest](jsonParser: JsonParser): T = {
    mapper.readValue[T](jsonParser)
  }

  def streamParse[T: Manifest](arrayName: String, input: InputStream): JsonStreamParseResult[T] = {
    JsonStreamParseResult.create(
      mapper = this,
      parser = mapper.getFactory.createParser(input),
      factory = mapper.getNodeFactory,
      arrayName = arrayName)
  }

  /** TODO: Handle escaped delimiters */
  def streamParseDelimited[T: Manifest](delimiter: Char, arrayName: String, reader: Reader): Iterator[JsonStreamParseResult[T]] = {
    for (delimitedReader <- new ReaderIterator(reader, delimiter)) yield {
      streamParse(arrayName, delimitedReader)
    }
  }

  def streamParse[T: Manifest](arrayName: String, reader: Reader): JsonStreamParseResult[T] = {
    JsonStreamParseResult.create(
      mapper = this,
      parser = mapper.getFactory.createParser(reader),
      factory = mapper.getNodeFactory,
      arrayName = arrayName)
  }

  def streamParse[T: Manifest](input: InputStream): Iterator[T] = {
    new JsonArrayIterator[T](
      this,
      mapper.getFactory.createParser(input))
  }

  def streamParse[T: Manifest](reader: Reader): Iterator[T] = {
    new JsonArrayIterator[T](
      this,
      mapper.getFactory.createParser(reader))
  }

  def convert[T: Manifest](any: Any): T = {
    mapper.convertValue[T](any)
  }

  def writeValueAsBytes(any: Any): Array[Byte] = {
    mapper.writeValueAsBytes(any)
  }

  def writeValue(any: Any, outputStream: OutputStream) {
    mapper.writeValue(outputStream, any)
  }

  def writeValueAsString(any: Any): String = {
    mapper.writeValueAsString(any)
  }

  def writePrettyString(any: Any): String = {
    val writer = new StringWriter()
    val generator = mapper.getFactory.createGenerator(writer)
    generator.useDefaultPrettyPrinter()
    generator.writeObject(any)
    writer.toString
  }

  def registerModule(module: Module) = {
    mapper.registerModule(module)
  }
}
