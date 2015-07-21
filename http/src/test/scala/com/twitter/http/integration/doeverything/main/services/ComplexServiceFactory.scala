package com.twitter.finatra.http.integration.doeverything.main.services

trait ComplexServiceFactory {
  def create(name: String): ComplexService
}
