package com.twitter.finatra.http.tests.integration.messagebody.main.domain

import com.twitter.finagle.http.MediaType
import com.twitter.finagle.http.Message
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.marshalling.MessageBodyWriter
import com.twitter.finatra.http.marshalling.WriterResponse
import com.twitter.util.jackson.ScalaObjectMapper
import javax.inject.Inject

class GreetingMessageBodyWriter @Inject() (mapper: ScalaObjectMapper)
    extends MessageBodyWriter[GreetingRequest] {

  override def write(greetingRequest: GreetingRequest): WriterResponse = {
    val greeting = getGreeting(greetingRequest.name, "en")
    writePlainResponse(greeting)
  }

  override def write(message: Message, greetingRequest: GreetingRequest): WriterResponse = {
    val greeting = getGreeting(greetingRequest.name, getLanguage(message))
    if (acceptsJson(message)) {
      writeJsonResponse(greeting)
    } else {
      writePlainResponse(greeting)
    }
  }

  private def acceptsJson(message: Message): Boolean = {
    message match {
      case req: Request =>
        req.acceptMediaTypes.contains("application/json")
      case _ => false
    }
  }

  private def getLanguage(message: Message): String = {
    message.headerMap.get("Accept-Language") match {
      case Some("es") => "es"
      case _ => "en"
    }
  }

  private def getGreeting(name: String, language: String): String = {
    language match {
      case "es" => "Hola " + name
      case _ => "Hello " + name
    }
  }

  private def writePlainResponse(greeting: String): WriterResponse = {
    WriterResponse(MediaType.PlainTextUtf8, greeting)
  }

  private def writeJsonResponse(greeting: String): WriterResponse = {
    WriterResponse(MediaType.JsonUtf8, mapper.writeValueAsBytes(Map("greeting" -> greeting)))
  }
}
