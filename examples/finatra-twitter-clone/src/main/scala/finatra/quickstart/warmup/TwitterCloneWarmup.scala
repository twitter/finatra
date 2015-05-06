package finatra.quickstart.warmup

import com.twitter.finatra.http.routing.HttpWarmup
import com.twitter.finatra.httpclient.RequestBuilder.{get, post}
import com.twitter.finatra.utils.Handler
import javax.inject.{Inject, Singleton}

@Singleton
class TwitterCloneWarmup @Inject()(
  httpWarmup: HttpWarmup)
  extends Handler {

  override def handle(): Unit = {
    httpWarmup.send(
      post("/tweets/").body("{}"),
      times = 5)

    httpWarmup.send(
      get("/tweets/123"),
      times = 5)
  }
}
