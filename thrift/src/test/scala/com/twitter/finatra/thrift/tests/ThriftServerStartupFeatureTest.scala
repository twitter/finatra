package com.twitter.finatra.thrift.tests

import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.tests.doeverything.controllers.DoNothingController
import com.twitter.finatra.thrift.{EmbeddedThriftServer, ThriftServer}
import com.twitter.inject.Test
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.util.NonFatal

class ThriftServerStartupFeatureTest extends Test {

  "fail startup" in {
    val server = new EmbeddedThriftServer(new ThriftServer {
      override val modules = Seq(ClientIdWhitelistModule)

      override def configureThrift(router: ThriftRouter): Unit = {
        router.add[DoNothingController]
        router.add[DoNothingController] // can't add two services
      }
    })

    try {
      intercept[AssertionError] {
        server.start()
      }
    }
    finally {
      server.close()
    }
  }

  "use default thrift port" in {
    val server = new EmbeddedTwitterServer(new ThriftServer {
      override val modules = Seq(ClientIdWhitelistModule)

      override def configureThrift(router: ThriftRouter): Unit = {
        router
          .add[DoNothingController]
      }
    })

    try {
      server.start()
    }
    catch {
      case NonFatal(e) =>
      // no-op: Since it's ok if we can't start on the default port since it may be already in use
    }
    finally {
      server.close()
    }
  }
}
