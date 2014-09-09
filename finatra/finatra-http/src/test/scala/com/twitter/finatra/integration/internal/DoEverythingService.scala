package com.twitter.finatra.integration.internal

import javax.inject.Singleton

@Singleton
class DoEverythingService {
  def doit = {
    "done"
  }
}
