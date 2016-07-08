package com.twitter.finatra.http.tests.integration.doeverything.main.services

trait ComplexServiceFactory {
  def create(name: String): ComplexService
}
