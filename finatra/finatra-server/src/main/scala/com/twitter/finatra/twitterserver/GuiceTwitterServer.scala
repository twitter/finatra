package com.twitter.finatra.twitterserver

import com.twitter.finatra.guice.GuiceApp
import com.twitter.finatra.utils.Logging
import com.twitter.server.Lifecycle.Warmup
import com.twitter.util.Await

trait GuiceTwitterServer
  extends TwitterServerWithPorts
  with GuiceApp
  with Warmup
  with Logging {

  /* Public */

  override final def main() {
    super.main() // Call GuiceApp.main() to create injector

    info("Enabling health port.")
    warmupComplete()

    info("Startup complete, server ready.")
    Await.ready(adminHttpServer)
  }
}
