package com.twitter.inject.server.tests

import com.twitter.finagle.builder.Server
import com.twitter.inject.Test
import com.twitter.inject.server.PortUtils
import com.twitter.util.Awaitable.CanAwait
import com.twitter.util.{Time, Duration}
import java.net.InetSocketAddress

class PortUtilsTest extends Test {

  "PortUtils" should {

    "getPort for Server" in {
      val server = new Server {
        /**
         * The address to which this server is bound.
         */
        override def boundAddress = new InetSocketAddress(9999)

        override protected def closeServer(deadline: Time) = ???

        /**
         * Support for `Await.result`. The use of the implicit permit is an
         * access control mechanism: only `Await.result` may call this
         * method.
         */
        override def result(timeout: Duration)(implicit permit: CanAwait) = ???

        /**
         * Is this Awaitable ready? In other words: would calling
         * [[com.twitter.util.Awaitable.ready Awaitable.ready]] block?
         */
        override def isReady(implicit permit: CanAwait) = ???

        /**
         * Support for `Await.ready`. The use of the implicit permit is an
         * access control mechanism: only `Await.ready` may call this
         * method.
         */
        override def ready(timeout: Duration)(implicit permit: CanAwait) = ???
      }

      try {
        PortUtils.getPort(server) should be(9999)
      } finally {
        server.close()
      }
    }
  }
}
