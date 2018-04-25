package com.twitter.finatra.httpclientv2

import com.twitter.finagle.Http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.service.{RetryBudget, RetryFilter, RetryPolicy}
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
  var retryPolicyOptions: Option[RetryPolicy[(Request, Try[Response])]] = None
  var defaultHeaders: Map[String, String] = Map()
  var budget: RetryBudget = RetryBudget()
  var policy: RetryPolicy[(Request, Try[Response])]

  def setHostname(name: String): HttpClientBuilder = {
    this.hostname = hostname
    this
  }

  def setRetryBudget(retryBudget: RetryBudget): HttpClientBuilder = {
    client = client.withRetryBudget(retryBudget)
    budget = retryBudget
    this
  }

  def setRetryPolicy(policy: RetryPolicy[(Request,Try[Response])]): HttpClientBuilder = {
    this.retryPolicyOptions = Some(policy)
    this.policy = policy
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

    val filteredService = retryPolicyOptions match {
      case Some(policy) =>
        new RetryFilter(
          retryPolicy = policy,
          timer = DefaultTimer,
          statsReceiver = LoadedStatsReceiver,
          retryBudget = budget).andThen(service)
      case _ =>
        service
    }

    new HttpClient(hostname = hostname, httpService = filteredService, retryPolicy = retryPolicyOptions, defaultHeaders = defaultHeaders)
  }
}