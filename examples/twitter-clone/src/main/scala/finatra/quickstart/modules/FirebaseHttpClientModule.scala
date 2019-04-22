package finatra.quickstart.modules

import com.twitter.finatra.httpclient.modules.HttpClientModule
import com.twitter.finatra.http.response.ResponseUtils._
import com.twitter.inject.conversions.time._
import com.twitter.inject.utils.RetryPolicyUtils._

object FirebaseHttpClientModule extends HttpClientModule {

  private val sslHostFlag = flag("firebase.host", "", "firebase hostname")

  override def sslHostname = Some(sslHostFlag())
  override val dest = "/s/firebaseio/finatra"

  override def retryPolicy =
    Some(
      exponentialRetry(
        start = 10.millis,
        multiplier = 2,
        numRetries = 3,
        shouldRetry = Http4xxOr5xxResponses
      )
    )
}
