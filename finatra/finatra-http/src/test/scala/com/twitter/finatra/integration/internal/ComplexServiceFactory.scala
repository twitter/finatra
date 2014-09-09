package com.twitter.finatra.integration.internal


trait ComplexServiceFactory {
  def create(name: String): ComplexService
}
