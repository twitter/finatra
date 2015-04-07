package com.twitter.inject.server

import com.twitter.app.Flaggable
import com.twitter.finagle.ListeningServer
import com.twitter.finagle.builder.{Server => BuilderServer}
import java.net.{InetAddress, InetSocketAddress, SocketAddress}

object PortUtils {

  def ephemeralLoopback: String = {
    loopbackAddress + ":0"
  }

  def loopbackAddress = {
    InetAddress.getLoopbackAddress.getHostAddress
  }

  def getPort(server: ListeningServer): Int = {
    server.boundAddress.asInstanceOf[InetSocketAddress].getPort
  }

  def getPort(server: BuilderServer): Int = {
    getSocketAddress(server).asInstanceOf[InetSocketAddress].getPort
  }

  def getSocketAddress(server: BuilderServer): SocketAddress = {
    server.localAddress
  }

  def parseAddr(addrStr: String): InetSocketAddress = {
    Flaggable.ofInetSocketAddress.parse(addrStr)
  }
}
