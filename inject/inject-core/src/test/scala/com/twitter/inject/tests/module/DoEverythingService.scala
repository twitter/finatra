package com.twitter.inject.tests.module

import jakarta.inject.Singleton

@Singleton
class DoEverythingService {

  def doit = {
    "done"
  }
}
