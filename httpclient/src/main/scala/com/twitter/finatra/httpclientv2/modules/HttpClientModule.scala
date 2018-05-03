package com.twitter.finatra.httpclientv2.modules

import com.google.inject.Provides
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.service.{ResponseClassifier, RetryPolicy}
import com.twitter.finatra.httpclientv2.{HttpClient}
import com.twitter.inject.TwitterModule
import com.twitter.util.Try
import javax.inject.Singleton

import com.twitter.finatra.httpclientv2.HttpClientBuilder

abstract class HttpClientModule extends TwitterModule {

  def dest: String

  def classifier: ResponseClassifier = ResponseClassifier.Default

  def hostname: String = ""

  def retryPolicy: RetryPolicy[(Request, Try[Response])] = ???

  def defaultHeaders: Map[String, String] = Map()

  def sslHostname: String = ""

  @Singleton
  @Provides
  def provideHttpClient(): HttpClient = {
    HttpClientBuilder.create()
      .withHostname(hostname)
      .withRetryPolicy(retryPolicy)
      .withDefaultHeaders(defaultHeaders)
      .withTls(sslHostname)
      .withResponseClassifier(classifier).newClient(dest)
  }



}
