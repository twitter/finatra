package com.twitter.inject.server

import com.twitter.inject.server.PortUtils.getPort
import com.twitter.server.AdminHttpServer
import java.net.SocketAddress

trait Ports
  extends com.twitter.app.App
  with AdminHttpServer {

  def httpAdminPort: Int = getPort(adminHttpServer)

  def httpExternalPort: Option[Int] = None

  def httpExternalSocketAddress: Option[SocketAddress] = None

  def httpsExternalPort: Option[Int] = None

  def thriftPort: Option[Int] = None
}