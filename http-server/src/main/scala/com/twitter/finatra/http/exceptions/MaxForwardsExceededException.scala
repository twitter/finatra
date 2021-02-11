package com.twitter.finatra.http.exceptions

/**
 * Denotes that Controller forwarding exceeded the maximum depth.
 *
 * @param maxDepth the maximum number of forwards allowed within the server.
 *
 * @see [[com.twitter.finatra.http.HttpForward]]
 * @see [[com.twitter.finatra.http.HttpRouter#withMaxRequestForwardingDepth]]
 */
class MaxForwardsExceededException private[finatra] (maxDepth: Int) extends Exception {
  override def getMessage: String =
    s"Exceeded maximum depth of $maxDepth for nested requests"
}
