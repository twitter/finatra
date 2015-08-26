package com.twitter.inject.tests.module

trait ComplexServiceFactory {
  def create(name: String): ComplexService
}
