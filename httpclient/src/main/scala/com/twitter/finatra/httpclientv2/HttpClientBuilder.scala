package com.twitter.finatra.httpclientv2

import com.twitter.finagle.Http

object HttpClientBuilder {
  def create(): HttpClientBuilder = {
    createWithClient(Http.client)
  }

  def createWithClient(client: Http.Client): HttpClientBuilder = {
    new HttpClientBuilder(client)
  }
}

class HttpClientBuilder(client: Http.Client) {

  def hostname: String = ""
  def retryPolicy: Option[RetryPolicy[Try[Response]]] = None
  def defaultHeaders: Map[String, String] = Map()

  def setHostname(name: String): HttpClientBuilder = {
    this.hostname = hostname
    this
  }

  def setRetryPolicy(retryPolicy: retryOption[RetryPolicy[Try[Response]]]): HttpClientBuilder = {
    this.retryPolicy = retryPolicy
    this
  }

  def setDefaultHeaders(defaultHeaders: Map[String, String] = Map()): HttpClientBuilder = {
    this.defaultHeaders = defaultHeaders
    this
  }

  def setTls(sslHostname: String): HttpClientBuilder = {
    client = client.withTls(sslHostname)
    this
  }

  def setResponseClassifier(classifier: Service.ResponseClassifier): HttpClientBuilder = {
    client = client.withResponseClassifier()
    this
  }

  def newClient(dest: String): HttpClient = {
    val service = client.newService(dest)
    new HttpClient(hostname = hostname, httpService = service, retryPolicy = retryPolicy, defaultHeaders = defaultHeaders)
  }
}