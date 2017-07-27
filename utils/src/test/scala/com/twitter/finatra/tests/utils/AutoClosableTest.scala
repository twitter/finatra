package com.twitter.finatra.tests.utils

import com.twitter.finatra.utils.AutoClosable
import com.twitter.inject.{Logging, Test}

class AutoClosableTest extends Test {

  test("AutoClosable#close") {
    val closable = new AutoClosableObject()

    AutoClosable.tryWith(closable) { closable =>
      closable.doSomething()
    }

    closable.isClosed should be(true)
  }

}

class AutoClosableObject extends AutoCloseable with Logging {
  private var closed = false

  def isClosed: Boolean = {
    closed
  }

  def doSomething(): Unit = {
    info("Performing auto-closable function.")
  }

  override def close(): Unit = {
    closed = true
  }
}
