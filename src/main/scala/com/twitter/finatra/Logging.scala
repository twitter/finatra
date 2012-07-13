package com.twitter.finatra

import com.twitter.logging.Logger

trait Logging {
  val logger = Logger.get("com.twitter.finatra")

  def appendCollection[A,B](buf: StringBuilder, x: Map[A,B]) {
    x foreach { xs =>
      buf.append(xs._1)
      buf.append(" : ")
      buf.append(xs._2)
    }
  }
}

