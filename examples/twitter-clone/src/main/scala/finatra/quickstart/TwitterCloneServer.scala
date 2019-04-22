package finatra.quickstart

import com.google.inject.Module
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.app.DtabResolution
import finatra.quickstart.controllers.TweetsController
import finatra.quickstart.modules.{FirebaseHttpClientModule, TwitterCloneJacksonModule}
import finatra.quickstart.warmup.TwitterCloneWarmupHandler

object TwitterCloneServerMain extends TwitterCloneServer

class TwitterCloneServer extends HttpServer with DtabResolution {
  override val modules: Seq[Module] =
    Seq(FirebaseHttpClientModule)

  override def jacksonModule: Module = TwitterCloneJacksonModule

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .add[TweetsController]
  }

  override protected def warmup(): Unit = {
    handle[TwitterCloneWarmupHandler]()
  }
}
