package com.twitter.inject.server

import com.twitter.app.Flaggable
import com.twitter.finagle.ListeningServer
import com.twitter.finagle.builder.{Server => BuilderServer}
import java.net.{InetAddress, InetSocketAddress, SocketAddress}

object PortUtils {

  def ephemeralLoopback: String = {
    loopbackAddressForPort(0)
  }

  def loopbackAddress = {
    InetAddress.getLoopbackAddress.getHostAddress
  }

  def loopbackAddressForPort(port: Int) = {
    s"$loopbackAddress:$port"
  }

  def getPort(server: ListeningServer): Int = {
    getPort(server.boundAddress)
  }

  def getPort(socketAddress: SocketAddress): Int = {
    socketAddress.asInstanceOf[InetSocketAddress].getPort
  }

  def getPort(server: BuilderServer): Int = {
    getSocketAddress(server).asInstanceOf[InetSocketAddress].getPort
  }

  def getSocketAddress(server: BuilderServer): SocketAddress = {
    server.boundAddress
  }

  def parseAddr(addrStr: String): InetSocketAddress = {
    Flaggable.ofInetSocketAddress.parse(addrStr)
  }
}
