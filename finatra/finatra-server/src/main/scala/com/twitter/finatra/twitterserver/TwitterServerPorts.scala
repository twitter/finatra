package com.twitter.finatra.twitterserver

import com.twitter.app.App
import com.twitter.finatra.utils.PortUtils._
import com.twitter.server.AdminHttpServer
import java.net.SocketAddress

trait TwitterServerPorts
  extends App
  with AdminHttpServer {

  def httpAdminPort: Int = getPort(adminHttpServer)

  def httpExternalPort: Option[Int] = None

  def httpExternalSocketAddress: Option[SocketAddress] = None

  def httpsExternalPort: Option[Int] = None

  def thriftPort: Option[Int] = None
}