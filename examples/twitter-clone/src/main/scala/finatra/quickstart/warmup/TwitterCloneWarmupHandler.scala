package finatra.quickstart.warmup

import com.twitter.finatra.http.routing.HttpWarmup
import com.twitter.finatra.httpclient.RequestBuilder.{get, post}
import com.twitter.inject.utils.Handler
import javax.inject.{Inject, Singleton}

@Singleton
class TwitterCloneWarmupHandler @Inject()(
  httpWarmup: HttpWarmup)
  extends Handler {

  override def handle(): Unit = {
    httpWarmup.send(
      post("/tweet/").body("{}"),
      times = 5)

    httpWarmup.send(
      get("/tweet/123"),
      times = 5)
  }
}
