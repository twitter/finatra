package com.twitter.finatra

package object twitterserver {

  @deprecated("Mix in both com.twitter.server.TwitterServer and com.twitter.inject.server.Ports", "")
  trait TwitterServerWithPorts extends com.twitter.server.TwitterServer with com.twitter.inject.server.Ports

  @deprecated("Use com.twitter.inject.server.Ports", "")
  trait TwitterServerPorts extends com.twitter.inject.server.Ports

  @deprecated("com.twitter.inject.server.TwitterServer", "")
  trait GuiceTwitterServer extends com.twitter.inject.server.TwitterServer
}
