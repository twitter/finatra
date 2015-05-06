package finatra.quickstart

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import finatra.quickstart.controllers.TweetsController
import finatra.quickstart.domain.StatusMessageBodyWriter
import finatra.quickstart.firebase.FirebaseHttpClientModule
import finatra.quickstart.warmup.TwitterCloneWarmup

class TwitterCloneServer extends HttpServer {
  override val resolveFinagleClientsOnStartup = true

  override val modules = Seq(FirebaseHttpClientModule)

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .register[StatusMessageBodyWriter]
      .filter[CommonFilters]
      .add[TweetsController]
  }

  override def warmup() {
    run[TwitterCloneWarmup]()
  }
}