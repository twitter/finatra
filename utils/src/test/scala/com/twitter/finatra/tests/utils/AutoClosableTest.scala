package com.twitter.finatra.tests.utils

import com.twitter.finatra.utils.AutoClosable
import com.twitter.inject.Test
import com.twitter.util.logging.Logger

class AutoClosableTest extends Test {

  test("AutoClosable#close") {
    val closable = new AutoClosableObject()

    AutoClosable.tryWith(closable) { closable =>
      closable.doSomething()
    }

    closable.isClosed should be(true)
  }

}

private object AutoClosableObject {
  val logger: Logger = Logger(AutoClosableObject.getClass)
}

class AutoClosableObject extends AutoCloseable {
  import AutoClosableObject._

  private var closed = false

  def isClosed: Boolean = {
    closed
  }

  def doSomething(): Unit = {
    logger.info("Performing auto-closable function.")
  }

  override def close(): Unit = {
    closed = true
  }
}
