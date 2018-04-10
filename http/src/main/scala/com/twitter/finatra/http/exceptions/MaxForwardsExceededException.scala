package com.twitter.finatra.http.exceptions

class MaxForwardsExceededException(maxDepth: Int) extends Exception {
  override def getMessage: String =
    s"Exceeded maximum depth of $maxDepth for nested requests"
}
