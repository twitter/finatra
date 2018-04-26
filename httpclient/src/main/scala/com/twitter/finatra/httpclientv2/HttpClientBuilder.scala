package com.twitter.finatra.httpclientv2

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.service.{ResponseClassifier, RetryBudget, RetryFilter, RetryPolicy}
import com.twitter.finagle.stats.LoadedStatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.Try

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
  var retryPolicy: Option[RetryPolicy[(Request, Try[Response])]] = None
  var budget: RetryBudget = RetryBudget()
  var policy: RetryPolicy[(Request, Try[Response])]
  var defaultHeaders: Map[String, String] = Map()
  var sslHostname: String = ""
  var classifier: ResponseClassifier = None

  def withHostname(name: String): HttpClientBuilder = {
    this.hostname = hostname
    this
  }

  def withRetryBudget(retryBudget: RetryBudget): HttpClientBuilder = {
    budget = retryBudget
    this
  }

  def withRetryPolicy(policy: RetryPolicy[(Request,Try[Response])]): HttpClientBuilder = {
    this.retryPolicy = Some(policy)
    this.policy = policy
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

  def withResponseClassifier(classifier: ResponseClassifier): HttpClientBuilder = {
    this.classifier = classifier
    this
  }

  def newClient(dest: String): HttpClient = {
    val service = client.withTls(sslHostname).withRetryBudget(budget).withResponseClassifier(classifier).newService(dest)


    val filteredService = retryPolicy match {
      case Some(policy) =>
        new RetryFilter(
          retryPolicy = policy,
          timer = DefaultTimer,
          statsReceiver = LoadedStatsReceiver,
          retryBudget = budget).andThen(service)
      case _ =>
        service
    }

    new HttpClient(hostname = hostname, httpService = filteredService, retryPolicy = retryPolicy, defaultHeaders = defaultHeaders)
  }
}