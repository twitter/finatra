package com.twitter.finatra.httpclient.modules

import com.google.inject.Provides
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.guice.GuiceModule
import com.twitter.finatra.httpclient.{HttpClient, RichHttpClient}
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.Singleton

abstract class HttpClientModule extends GuiceModule {

  val clientName: String

  @Singleton
  @Provides
  def provideHttpClient(
    mapper: FinatraObjectMapper,
    httpService: Service[Request, Response]): HttpClient = {

    new HttpClient(
      httpService = httpService,
      mapper = mapper)
  }

  @Singleton
  @Provides
  def provideHttpService: Service[Request, Response] = {
    RichHttpClient.newClientService(
      target = "%s=flag!%s".format(clientName, clientName))
  }
}
