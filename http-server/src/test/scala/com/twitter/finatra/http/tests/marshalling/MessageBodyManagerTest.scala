package com.twitter.finatra.http.tests.marshalling

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finagle.http.{Fields, MediaType, Message, Request, Response}
import com.twitter.finatra.http.annotations.{Header, QueryParam}
import com.twitter.finatra.http.marshalling._
import com.twitter.finatra.http.modules.MessageBodyModule
import com.twitter.finatra.http.tests.integration.json.CaseClassWithBoolean
import com.twitter.finatra.http.{Prod, TestMessageBodyWriterAnn}
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.finatra.modules.FileResolverModule
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, Test}
import com.twitter.util.jackson.ScalaObjectMapper
import com.twitter.util.jackson.caseclass.exceptions.InjectableValuesException
import com.twitter.util.mock.Mockito
import javax.inject.Inject

private object MessageBodyManagerTest {
  trait Car {
    def name: String
  }

  case class FooCar(name: String) extends Car
  case class BarCar(name: String) extends Car
  case class Car2(name: String)
  case class Dog(name: String)
  @TestMessageBodyWriterAnn case class TestClass(name: String)
  case class NoReaderRegistered(id: Long)
  case class NoWriterRegistered(id: Long)
  case class TestClassWithResponse(@Header `content-type`: String, response: Response)
  case class TestClassWithResponseToo(@QueryParam q: String, response: Response)

  class ReallyCoolMessageBodyComponent extends MessageBodyComponent

  class Car2MessageBodyReader extends MessageBodyReader[Car2] {
    override def parse(message: Message): Car2 = Car2("Car")
  }

  class DogMessageBodyReader extends MessageBodyReader[Dog] {
    override def parse(message: Message): Dog = Dog("Dog")
  }

  class MapIntDoubleMessageBodyReader extends MessageBodyReader[Map[Int, Double]] {
    override def parse(message: Message): Map[Int, Double] =
      Map(1 -> 0.0, 2 -> 3.14, 3 -> 0.577)
  }

  class CarMessageBodyWriter @Inject() (mapper: ScalaObjectMapper) extends MessageBodyWriter[Car] {
    override def write(car: Car): WriterResponse = {
      WriterResponse(MediaType.JsonUtf8, mapper.writeValueAsBytes(Map("name" -> car.name)))
    }
  }

  class TestMessageBodyWriter extends MessageBodyWriter[Any] {
    override def write(obj: Any): WriterResponse = {
      WriterResponse(MediaType.PlainTextUtf8, obj.toString)
    }
  }

  class ReallyCoolMessageBodyWriter extends MessageBodyWriter[Any] {
    override def write(obj: Any): WriterResponse = {
      WriterResponse(MediaType.PlainTextUtf8, "this is really cool.")
    }
  }
}

// note: these tests can be removed once we remove the mutable methods from MessageBodyManager
class MessageBodyManagerTest extends Test with Mockito {
  import MessageBodyManagerTest._

  private val message: Message = mock[Message]
  private val injector: Injector =
    TestInjector(FileResolverModule, MessageBodyModule, ScalaObjectMapperModule).create

  private val messageBodyManager: MessageBodyManager = injector.instance[MessageBodyManager]
  messageBodyManager.add[DogMessageBodyReader]()
  messageBodyManager.add[Car2MessageBodyReader]()
  messageBodyManager.addExplicit[CarMessageBodyWriter, FooCar]()
  messageBodyManager.addExplicit[CarMessageBodyWriter, BarCar]()
  messageBodyManager.addWriterByAnnotation[TestMessageBodyWriterAnn, TestMessageBodyWriter]()
  messageBodyManager
    .addWriterByComponentType[ReallyCoolMessageBodyComponent, ReallyCoolMessageBodyWriter]()

  private val defaultMessageBodyReader: DefaultMessageBodyReader =
    injector.instance[DefaultMessageBodyReader]
  private val mapper: ScalaObjectMapper = injector.instance[ScalaObjectMapper]

  test("adding by writer annotation for an annotation without MessageBodyWriter annotation fails") {
    intercept[AssertionError] {
      messageBodyManager.addWriterByAnnotation[Prod, TestMessageBodyWriter]()
    }
  }

  test("parse dog") {
    messageBodyManager.read[Dog](message) should equal(Dog("Dog"))
  }

  test("parse car2") {
    messageBodyManager.read[Car2](message) should equal(Car2("Car"))
  }

  test("parse map with MBR not supported") {
    intercept[IllegalArgumentException] {
      messageBodyManager.add[MapIntDoubleMessageBodyReader]()
    }
  }

  test("parse request body into type") {
    val request = Request()
    request.setContentString("""{ "foo": "true" }""")
    val result = MessageBodyReader.parseMessageBody[CaseClassWithBoolean](
      request,
      mapper.underlying.readerFor[CaseClassWithBoolean]
    )
    assert(result == CaseClassWithBoolean(true))
  }

  test("parse request body into type with default MBR") {
    val request = Request()
    val json = """{ "foo": "true" }"""
    request.setContentString(json)
    request.headerMap.set(Fields.ContentLength, json.length.toString)
    request.headerMap.set(Fields.ContentType, MediaType.Json)
    val result = defaultMessageBodyReader.parse[CaseClassWithBoolean](request)
    assert(result == CaseClassWithBoolean(true))
  }

  test("parse request") {
    val request = Request()
    request.setContentString("""{"msg": "hi"}""")
    val jsonNode = MessageBodyReader.parseMessageBody[JsonNode](
      request,
      mapper.underlying.readerFor[JsonNode]
    )
    jsonNode.get("msg").textValue() should equal("hi")
  }

  test("parse request with default MBR") {
    val request = Request()
    val json = """{"msg": "hi"}"""
    request.setContentString(json)
    request.headerMap.set(Fields.ContentLength, json.length.toString)
    request.headerMap.set(Fields.ContentType, MediaType.Json)
    val jsonNode = defaultMessageBodyReader.parse[JsonNode](request)
    jsonNode.get("msg").textValue() should equal("hi")
  }

  test("parse response") {
    val response = Response()
    response.setContentString("""{"msg": "hi"}""")
    val jsonNode = MessageBodyReader
      .parseMessageBody[JsonNode](
        response,
        mapper.underlying.readerFor[JsonNode]
      )
    jsonNode.get("msg").textValue() should equal("hi")
  }

  test("parse response with default MBR") {
    val response = Response()
    val json = """{"msg": "hi"}"""
    response.setContentString(json)
    response.headerMap.set(Fields.ContentLength, json.length.toString)
    response.headerMap.set(Fields.ContentType, MediaType.Json)

    val jsonNode = defaultMessageBodyReader.parse[JsonNode](response)
    jsonNode.get("msg").textValue() should equal("hi")
  }

  test("parse response case class") {
    val response = Response()
    val json = """{"msg": "hi"}"""
    response.setContentString(json)
    response.headerMap.set(Fields.ContentLength, json.length.toString)
    response.headerMap.set(Fields.ContentType, MediaType.Json)

    val tcwr: TestClassWithResponse =
      defaultMessageBodyReader.parse[TestClassWithResponse](response)
    tcwr.response should not be (null)
    tcwr.`content-type` should equal(MediaType.Json)
  }

  test("parse response case class with request annotation fails") {
    val response = Response()
    val json = """{"msg": "hi"}"""
    response.setContentString(json)
    response.headerMap.set(Fields.ContentLength, json.length.toString)
    response.headerMap.set(Fields.ContentType, MediaType.Json)

    intercept[InjectableValuesException] {
      defaultMessageBodyReader.parse[TestClassWithResponseToo](response)
    }
  }

  test("find no reader for type") {
    val reader = messageBodyManager.reader[NoReaderRegistered]
    reader should be(None)
  }

  test("find writer for foo car") {
    val fooCar = FooCar("foo")
    val writer = messageBodyManager.writer(fooCar)
    writer should not be (null)
    writer.isInstanceOf[CarMessageBodyWriter] should be(true)
    val writerResponse = writer.write(fooCar)
    writerResponse.contentType should equal(MediaType.JsonUtf8)
    mapper.parse[FooCar](writerResponse.body.asInstanceOf[Array[Byte]]) should equal(fooCar)
  }

  test("find writer for bar car") {
    val barCar = BarCar("bar")
    val writer = messageBodyManager.writer(barCar)
    writer should not be (null)
    writer.isInstanceOf[CarMessageBodyWriter] should be(true)
    val writerResponse = writer.write(barCar)
    writerResponse.contentType should equal(MediaType.JsonUtf8)
    mapper.parse[BarCar](writerResponse.body.asInstanceOf[Array[Byte]]) should equal(barCar)
  }

  test("find writer for TestMessageBodyWriterAnn") {
    val testClazz = TestClass("Wilbert")
    val writer = messageBodyManager.writer(testClazz)
    writer should not be (null)
    writer.isInstanceOf[TestMessageBodyWriter] should be(true)
    val writerResponse = writer.write(testClazz)
    writerResponse.contentType should equal(MediaType.PlainTextUtf8)
    writerResponse.body should equal(testClazz.toString)
  }

  test("find writer for ReallyCoolMessageBodyComponent") {
    val reallyCoolComponent = new ReallyCoolMessageBodyComponent
    val writer = messageBodyManager.writer(reallyCoolComponent)
    writer should not be (null)
    writer.isInstanceOf[ReallyCoolMessageBodyWriter] should be(true)
    val writerResponse = writer.write(reallyCoolComponent)
    writerResponse.contentType should equal(MediaType.PlainTextUtf8)
    writerResponse.body should equal("this is really cool.")
  }

  test("find default writer for instance") {
    val instance = NoWriterRegistered(1234L)
    val writer = messageBodyManager.writer(instance)
    writer should not be (null)
    writer.isInstanceOf[DefaultMessageBodyWriter] should be(true)
  }
}
