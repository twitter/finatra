package com.twitter.finatra.httpclient.modules

abstract class HttpClientModule extends TwitterModule {

  def dest: String

  def sslHostname: Option[String] = None

  def classifier: Service.ResponseClassifier = None

  def hostname: String = ""

  def retryPolicy: Option[RetryPolicy[Try[Response]]] = None

  def defaultHeaders: Map[String, String] = Map()

  def sslHostname: String = ""

  @Singleton
  @Provides
  def provideHttpClient(): HttpClient = {
    return HttpClientBuilder.create()
      .withHostname(hostname)
      .withRetryPolicy(retryPolicy)
      .withDefaultHeaders(defaultHeaders)
      .withTls(sslHostname)
      .withResponseClassifier(classifier).newClient(dest)
  }

}
