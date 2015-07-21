package finatra.quickstart.firebase

import com.twitter.finatra.conversions.time._
import com.twitter.finatra.httpclient.modules.HttpClientModule
import com.twitter.finatra.utils.RetryPolicyUtils._

object FirebaseHttpClientModule extends HttpClientModule {

  private val sslHostFlag = flag("firebase.host", "", "firebase hostname")

  override def sslHostname = Some(sslHostFlag())
  override val dest = "flag!firebase"

  override def retryPolicy = Some(exponentialRetry(
    start = 10.millis,
    multiplier = 2,
    numRetries = 3,
    shouldRetry = Http4xxOr5xxResponses))
}
