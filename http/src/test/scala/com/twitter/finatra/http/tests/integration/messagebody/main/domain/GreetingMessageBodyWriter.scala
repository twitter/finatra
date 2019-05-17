package com.twitter.finatra.http.tests.integration.messagebody.main.domain

import com.twitter.finagle.http.MediaType
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.marshalling.{MessageBodyWriter, WriterResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.Inject

class GreetingMessageBodyWriter @Inject()(mapper: FinatraObjectMapper)
    extends MessageBodyWriter[GreetingRequest] {

  override def write(greetingRequest: GreetingRequest): WriterResponse = {
    val greeting = getGreeting(greetingRequest.name, "en")
    writePlainResponse(greeting)
  }

  override def write(request: Request, greetingRequest: GreetingRequest): WriterResponse = {
    val greeting = getGreeting(greetingRequest.name, getLanguage(request))
    if (acceptsJson(request)) {
      writeJsonResponse(greeting)
    } else {
      writePlainResponse(greeting)
    }
  }

  private def acceptsJson(request: Request): Boolean = {
    request.acceptMediaTypes.contains("application/json")
  }

  private def getLanguage(request: Request): String = {
    request.headerMap.get("Accept-Language") match {
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
