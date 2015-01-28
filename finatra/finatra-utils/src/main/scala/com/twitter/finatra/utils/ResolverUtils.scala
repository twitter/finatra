package com.twitter.finatra.utils

import com.twitter.finagle.client.ClientRegistry
import com.twitter.util.Await

object ResolverUtils extends Logging {

  def waitUntilAllClientsAreResolved() {
    Await.ready {
      ClientRegistry.expAllRegisteredClientsResolved() onSuccess { clients =>
        info("Done resolving clients: " + clients.mkString("[", ", ", "]") + ".")
      }
    }
  }
}
