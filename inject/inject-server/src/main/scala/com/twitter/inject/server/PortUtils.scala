package com.twitter.inject.server

import com.twitter.app.Flaggable
import com.twitter.finagle.ListeningServer
import java.net.{InetAddress, InetSocketAddress, SocketAddress}

/**
 * Utilities for formatting Integer ports and SocketAddresses.
 */
object PortUtils {

  /** Returns a `host:port` String which is the loopback address and the emphemeral port :0 */
  def ephemeralLoopback: String = {
    loopbackAddressForPort(0)
  }

  /**
   * Returns the loopback Inet Address.
   *
   * @see [[java.net.InetAddress.getLoopbackAddress]]
   */
  def loopbackAddress: String = {
    InetAddress.getLoopbackAddress.getHostAddress
  }

  /** Returns a `host:port` String for the loopback address and the given port. */
  def loopbackAddressForPort(port: Int): String = {
    s"$loopbackAddress:$port"
  }

  /** Returns the Integer representation of the bound address for the given [[ListeningServer]] */
  def getPort(server: ListeningServer): Int = {
    getPort(server.boundAddress)
  }

  /** Returns the Integer representation of the given [[SocketAddress]] */
  def getPort(socketAddress: SocketAddress): Int = {
    socketAddress.asInstanceOf[InetSocketAddress].getPort
  }

  /** Returns the bound address of the given [[ListeningServer]] */
  def getSocketAddress(server: ListeningServer): SocketAddress = {
    server.boundAddress
  }

  /**
   * Parses the given String as an InetSocketAddress using [[com.twitter.app.Flaggable]]
   * @see [[com.twitter.app.Flaggable.ofInetSocketAddress]]
   */
  def parseAddr(addrStr: String): InetSocketAddress = {
    Flaggable.ofInetSocketAddress.parse(addrStr)
  }
}
