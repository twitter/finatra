package com.twitter.finatra.http.tests.integration.doeverything.main.services

import jakarta.inject.Singleton

@Singleton
class DoEverythingService {

  def doit: String = "done"
}
