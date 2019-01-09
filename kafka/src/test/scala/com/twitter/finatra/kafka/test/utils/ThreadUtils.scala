package com.twitter.finatra.kafka.test.utils

object ThreadUtils {

  def fork(func: => Unit): Unit = {
    new Thread {
      override def run(): Unit = {
        func
      }
    }.start()
  }

}
