package com.twitter.finatra

import com.twitter.finagle.ListeningServer
import com.twitter.inject.server.{PortUtils => NewPortUtils}
import java.net.InetSocketAddress

package object utils {

  @deprecated("Use com.twitter.inject.Logging", "2015-03-24")
  type Logging = com.twitter.inject.Logging

  @deprecated("Use com.twitter.inject.server.PortUtils", "2015-03-24")
  object PortUtils {
    def ephemeralLoopback: String = {
      NewPortUtils.ephemeralLoopback
    }

    def loopbackAddress = {
       NewPortUtils.loopbackAddress
    }

    def getPort(server: ListeningServer): Int = {
       NewPortUtils.getPort(server)
    }

    def parseAddr(addrStr: String): InetSocketAddress = {
       NewPortUtils.parseAddr(addrStr)
    }
  }
}
