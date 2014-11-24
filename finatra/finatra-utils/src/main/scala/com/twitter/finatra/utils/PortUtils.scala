package com.twitter.finatra.utils

import com.twitter.app.Flaggable
import com.twitter.finagle.ListeningServer
import com.twitter.finagle.builder.{Server => BuilderServer}
import java.net.{InetAddress, InetSocketAddress, SocketAddress}

object PortUtils {
  def getLoopbackHostAddress: String =
    InetAddress.getLoopbackAddress.getHostAddress
  
  def getPort(server: ListeningServer): Int = {
    server.boundAddress.asInstanceOf[InetSocketAddress].getPort
  }

  def getPort(server: BuilderServer): Int = {
    getSocketAddress(server).asInstanceOf[InetSocketAddress].getPort
  }

  def getSocketAddress(server: BuilderServer): SocketAddress = {
    server.localAddress
  }

  def getPort(socketAddress: SocketAddress): Int = {
    socketAddress.asInstanceOf[InetSocketAddress].getPort
  }

  def parseAddr(addrStr: String): InetSocketAddress = {
    Flaggable.ofInetSocketAddress.parse(addrStr)
  }
}
