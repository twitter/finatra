package com.twitter.finatra.utils

object AutoClosable {

  def tryWith[AC <: AutoCloseable, T](ac: AC)(func: AC => T): T = {
    try {
      func(ac)
    } finally {
      ac.close()
    }
  }
}
