package com.twitter.finatra.utils

import com.twitter.app.Flaggable
import com.twitter.finagle.ListeningServer
import java.net.{InetAddress, InetSocketAddress, SocketAddress}

object PortUtils {
  def getLoopbackHostAddress: String =
    InetAddress.getLoopbackAddress.getHostAddress

  def getPort(server: ListeningServer): Int = {
    server.boundAddress.asInstanceOf[InetSocketAddress].getPort
  }

  def getSocketAddress(server: ListeningServer): SocketAddress = {
    server.boundAddress
  }

  def getPort(socketAddress: SocketAddress): Int = {
    socketAddress.asInstanceOf[InetSocketAddress].getPort
  }

  def parseAddr(addrStr: String): InetSocketAddress = {
    Flaggable.ofInetSocketAddress.parse(addrStr)
  }
}
