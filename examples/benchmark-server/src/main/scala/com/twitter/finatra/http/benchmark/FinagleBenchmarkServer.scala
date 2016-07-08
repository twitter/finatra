package com.twitter.finatra.http.benchmark

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finagle.Service
import com.twitter.finagle.http.path.{Path, Root, _}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.internal.server.BaseHttpServer
import com.twitter.util.Future

class FinagleBenchmarkServer extends BaseHttpServer {

  private val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

  override def httpService = new Service[Request, Response] {
    override def apply(request: Request): Future[Response] = {
      Path(request.path) match {
        case Root / "json" =>
          val jsonMap = Map("message" -> "Hello, World!")
          createJsonResponse(jsonMap)
        case Root / "json" / id =>
          val jsonMap = Map("message" -> ("Hello " + id))
          createJsonResponse(jsonMap)
        case Root / "hi" =>
          val response = Response(Status.Ok)
          val msg = "Hello " + request.params.getOrElse("name", "unnamed")
          response.setContentString(msg)
          Future(response)
        case _ =>
          Future(Response(Status.NotFound))
      }
    }
  }

  private def createJsonResponse(jsonMap: Map[String, String]): Future[Response] = {
    val response = Response(Status.Ok)
    response.setContentString(objectMapper.writeValueAsString(jsonMap))
    response.setContentTypeJson()
    Future(response)
  }
}
