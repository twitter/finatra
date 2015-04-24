package com.twitter.finatra.integration.doeverything.main.services

import javax.inject.Singleton

@Singleton
class DoEverythingService {
  def doit = {
    "done"
  }
}
