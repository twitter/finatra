package com.twitter.inject.tests.module

import javax.inject.Singleton

@Singleton
class DoEverythingService {

  def doit = {
    "done"
  }
}
