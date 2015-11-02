package com.twitter.finatra

package object twitterserver {

  @deprecated("Use com.twitter.inject.server.Ports", "")
  trait TwitterServerPorts extends com.twitter.inject.server.Ports

  @deprecated("com.twitter.inject.server.TwitterServer", "")
  trait GuiceTwitterServer extends com.twitter.inject.server.TwitterServer
}
