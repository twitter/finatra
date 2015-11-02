package com.twitter.inject.server

import java.net.SocketAddress

trait Ports
  extends com.twitter.server.TwitterServer {

  def httpExternalPort: Option[Int] = None

  def httpExternalSocketAddress: Option[SocketAddress] = None

  def httpsExternalPort: Option[Int] = None

  def thriftPort: Option[Int] = None
}