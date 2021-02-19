package com.twitter.finatra.httpclient.modules

import com.google.inject.Provides
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{Http, Resolvers, Service}
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.inject.Injector
import javax.inject.Singleton

@deprecated(
  "Please use com.twitter.finatra.httpclient.modules.HttpClientModuleTrait directly.",
  "07-08-2019")
abstract class HttpClientModule extends HttpClientModuleTrait {

  // for backwards compatibility purposes, mirrors the behavior of a client w/o label specified
  override def label: String = Resolvers.evalLabeled(dest)._2

  def sslHostname: Option[String] = None

  override def configureClient(
    injector: Injector,
    client: Http.Client
  ): Http.Client = sslHostname match {
    case Some(sslHost) => client.withTls(sslHost)
    case _ => client
  }

  @Singleton
  @Provides
  final def provideHttpClient(
    mapper: ScalaObjectMapper,
    httpService: Service[Request, Response]
  ): HttpClient = {

    new HttpClient(
      hostname = hostname,
      httpService = httpService,
      retryPolicy = retryPolicy,
      defaultHeaders = defaultHeaders,
      mapper = mapper
    )
  }

  @Singleton
  @Provides
  final def provideHttpService(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): Service[Request, Response] = newService(injector, statsReceiver)
}
