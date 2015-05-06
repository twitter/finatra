package com.twitter.finatra.httpclient.modules

import com.google.inject.Provides
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finatra.httpclient.{HttpClient, RichHttpClient}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.TwitterModule
import com.twitter.util.Try
import javax.inject.Singleton

abstract class HttpClientModule extends TwitterModule {

  def dest: String

  def hostname: String = ""

  def retryPolicy: Option[RetryPolicy[Try[Response]]] = None

  def defaultHeaders: Map[String, String] = Map()

  def sslHostname: Option[String] = None

  @Singleton
  @Provides
  def provideHttpClient(
    mapper: FinatraObjectMapper,
    httpService: Service[Request, Response]): HttpClient = {

    new HttpClient(
      hostname = hostname,
      httpService = httpService,
      retryPolicy = retryPolicy,
      defaultHeaders = defaultHeaders,
      mapper = mapper)
  }

  @Singleton
  @Provides
  def provideHttpService: Service[Request, Response] = {
    sslHostname match {
      case Some(ssl) =>
        RichHttpClient.newSslClientService(
          sslHostname = ssl,
          dest = dest)
      case _ =>
        RichHttpClient.newClientService(
          dest = dest)
    }
  }
}
