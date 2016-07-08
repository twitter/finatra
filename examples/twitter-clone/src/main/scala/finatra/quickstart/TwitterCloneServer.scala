package finatra.quickstart

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import finatra.quickstart.controllers.TweetsController
import finatra.quickstart.modules.{FirebaseHttpClientModule, TwitterCloneJacksonModule}
import finatra.quickstart.warmup.TwitterCloneWarmupHandler

object TwitterCloneServerMain extends TwitterCloneServer

class TwitterCloneServer extends HttpServer {
  override val modules = Seq(FirebaseHttpClientModule)

  override def jacksonModule = TwitterCloneJacksonModule

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .add[TweetsController]
  }

  override def warmup() {
    handle[TwitterCloneWarmupHandler]()
  }
}