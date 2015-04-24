package com.twitter.finatra.integration.doeverything.main.services

trait ComplexServiceFactory {
  def create(name: String): ComplexService
}
