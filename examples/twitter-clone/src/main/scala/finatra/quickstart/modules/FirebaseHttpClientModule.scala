package finatra.quickstart.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.httpclient.modules.HttpClientModuleTrait
import com.twitter.finatra.http.response.ResponseUtils._
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.inject.Injector
import com.twitter.inject.utils.RetryPolicyUtils._

object FirebaseHttpClientModule extends HttpClientModuleTrait {

  private val sslHostFlag = flag("firebase.host", "", "firebase hostname")

  override val dest = "/s/firebaseio/finatra"
  override val label = "firebase"

  override def retryPolicy =
    Some(
      exponentialRetry(
        start = 10.millis,
        multiplier = 2,
        numRetries = 3,
        shouldRetry = Http4xxOr5xxResponses
      )
    )

  override protected def configureClient(
    injector: Injector,
    client: Http.Client
  ): Http.Client = client.withTls(sslHostFlag())

  @Provides
  @Singleton
  final def provideHttpClient(
    injector: Injector,
    statsReceiver: StatsReceiver,
    mapper: ScalaObjectMapper
  ): HttpClient = newHttpClient(injector, statsReceiver, mapper)
}
