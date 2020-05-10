package com.twitter.finatra.thrift.tests

import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.tests.doeverything.controllers.DoNothingController
import com.twitter.finatra.thrift.{EmbeddedThriftServer, ThriftServer}
import com.twitter.inject.Test
import com.twitter.inject.server.EmbeddedTwitterServer
import java.net.InetSocketAddress
import scala.util.control.NonFatal

class ThriftServerStartupFeatureTest extends Test {

  test("fail startup") {
    val server = new EmbeddedThriftServer(
      new ThriftServer {
        override def configureThrift(router: ThriftRouter): Unit = {
          router.add[DoNothingController]
          router.add[DoNothingController] // can't add two services
        }
      },
      disableTestLogging = true)

    try {
      intercept[AssertionError] {
        server.start()
      }
    } finally {
      server.close()
    }
  }

  test("use default thrift port") {
    val server = new EmbeddedTwitterServer(
      new ThriftServer {
        override def configureThrift(router: ThriftRouter): Unit = {
          router
            .add[DoNothingController]
        }
      },
      disableTestLogging = true)

    try {
      server.start()
    } catch {
      case NonFatal(e) =>
      // no-op: Since it's ok if we can't start on the default port since it may be already in use
    } finally {
      server.close()
    }
  }

  test("start with directly instantiated controller") {
    val server = new EmbeddedTwitterServer(
      new ThriftServer {
        override def configureThrift(router: ThriftRouter): Unit = {
          router
          // directly instantiate controller and add it to the router
            .add(new DoNothingController())
        }
      },
      disableTestLogging = true)

    try {
      server.start()
    } catch {
      case NonFatal(e) =>
      // no-op: Since it's ok if we can't start on the default port since it may be already in use
    } finally {
      server.close()
    }
  }

  test("Thrift Server test if ListeningServer address is bound in postWarmup") {
    var boundAddressBeforePostWarmup: Option[InetSocketAddress] = None
    var boundAddressPostWarmup: Option[InetSocketAddress] = None
    val server = new EmbeddedTwitterServer(
      new ThriftServer {
        override def configureThrift(router: ThriftRouter): Unit = {
          router
            .add[DoNothingController]
        }

        override protected def postInjectorStartup(): Unit = {
          super.postInjectorStartup()
          boundAddressBeforePostWarmup = boundAddress
        }

        override def postWarmup(): Unit = {
          super.postWarmup();
          boundAddressPostWarmup = boundAddress
        }
      },
      disableTestLogging = true)

    try {
      server.start()
      // Testing value of boundAddress before postWarmup and after postWarmup
      assert(boundAddressBeforePostWarmup.isEmpty)
      assert(boundAddressPostWarmup.isDefined)
    } finally {
      server.close()
    }
  }
}
