package com.twitter.inject.server

trait Ports
  extends com.twitter.server.TwitterServer {

  def httpExternalPort: Option[Int] = None

  def httpsExternalPort: Option[Int] = None

  def thriftPort: Option[Int] = None
}