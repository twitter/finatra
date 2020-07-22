package com.twitter.inject.app

class StartupTimeoutException(message: String, cause: Throwable) extends Exception(message, cause) {

  def this(message: String) {
    this(message, null)
  }
}
