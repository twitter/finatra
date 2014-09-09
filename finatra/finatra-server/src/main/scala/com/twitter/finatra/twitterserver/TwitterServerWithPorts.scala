package com.twitter.finatra.twitterserver

import com.twitter.server.TwitterServer

/**
 * TwitterServer that exposes the bound http and thrift ports
 */
trait TwitterServerWithPorts
  extends TwitterServer
  with TwitterServerPorts
