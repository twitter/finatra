package com.twitter.finatra

import com.twitter.app.App
import com.twitter.finagle.{Http, NullServer, ListeningServer}
import com.twitter.finagle.http.HttpMuxer

trait AdminHttpServer { self: App =>

  @volatile protected var adminHttpServer: ListeningServer = NullServer

  premain {
    adminHttpServer = Http.serve(config.adminPort(), HttpMuxer)
  }

  onExit {
    adminHttpServer.close()
  }
}
