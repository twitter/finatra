package com.twitter.finatra.utils

import scala.util.control.Exception._

object AutoClosable {

  def tryWith[AC <: AutoCloseable, T](ac: AC)(func: AC => T): T = {
    allCatch andFinally ac.close() apply func(ac)
  }
}
