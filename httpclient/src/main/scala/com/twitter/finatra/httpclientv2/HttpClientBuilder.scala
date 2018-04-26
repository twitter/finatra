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

  var hostname: String = ""
  var retryPolicy: Option[RetryPolicy[Try[Response]]] = None
  var defaultHeaders: Map[String, String] = Map()
  var sslHostname: String = ""
  var classifier: Service.ResponseClassifier = None

  def withHostname(name: String): HttpClientBuilder = {
    this.hostname = hostname
    this
  }

  def withRetryPolicy(retryPolicy: retryOption[RetryPolicy[Try[Response]]]): HttpClientBuilder = {
    this.retryPolicy = retryPolicy
    this
  }

  def withDefaultHeaders(defaultHeaders: Map[String, String] = Map()): HttpClientBuilder = {
    this.defaultHeaders = defaultHeaders
    this
  }

  def withTls(sslHostname: String): HttpClientBuilder = {
    this.sslHostname = sslHostname
    this
  }

  def withResponseClassifier(classifier: Service.ResponseClassifier): HttpClientBuilder = {
    this.classifier = classifier
    this
  }

  def newClient(dest: String): HttpClient = {
    val service = client.withTls(sslHostname).withResponseClassifier(classifier).newService(dest)
    new HttpClient(hostname = hostname, httpService = service, retryPolicy = retryPolicy, defaultHeaders = defaultHeaders)
  }
}