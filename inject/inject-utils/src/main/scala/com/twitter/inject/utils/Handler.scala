package com.twitter.inject.utils

abstract class Handler {

  /** Executes the function of this handler. **/
  def handle(): Unit
}
