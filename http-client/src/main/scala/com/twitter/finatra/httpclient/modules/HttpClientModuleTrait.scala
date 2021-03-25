package com.twitter.finatra.httpclient.modules

import com.twitter.finagle.Http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.inject.Injector
import com.twitter.inject.modules.StackClientModuleTrait
import com.twitter.util.Try

/**
 * Extending this trait allows for configuring an
 * [[com.twitter.finagle.Http.Client]] and/or a [[com.twitter.finatra.httpclient.HttpClient]].
 *
 * @example
 *          {{{
 *            class MyHttpClientModule
 *              extends HttpClientModuleTrait {
 *
 *              override def dest: String = "/s/my/service"
 *              override def label: String = "myservice"
 *
 *              override protected def sessionAcquisitionTimeout: Duration = 1.seconds
 *              override protected def requestTimeout: Duration = 5.seconds
 *              override protected def retryBudget: RetryBudget = RetryBudget(15.seconds, 5, .1)
 *
 *              // if you want to customize the client configuration
 *              // you can:
 *              //
 *              // override def configureClient(
 *              //  injector: Injector,
 *              //  client: Http.Client
 *              // ): Http.Client =
 *              //   client.
 *              //     withTracer(NullTracer)
 *              //     withStatsReceiver(NullStatsReceiver)
 *              //
 *              // You can configure TLS on the client directly
 *              //
 *              // override def configureClient(
 *              //  injector: Injector,
 *              //  client: Http.Client
 *              // ): Http.Client = client.withTls("yourSslHostName")
 *              //
 *              // depending on your client type, you may want to provide a global instance,
 *              // otherwise you might want to specify how your consumers can provide a binding
 *              // for an instance to the client
 *              //
 *              // ex:
 *              // @Provides
 *              // @Singleton
 *              // final def provideFinagleClient(
 *              //   injector: Injector,
 *              //   statsReceiver: StatsReceiver
 *              // ): Http.Client = newClient(injector, statsReceiver)
 *              //
 *              // Or create a service directly
 *              //
 *              // ex:
 *              // @Provides
 *              // @Singleton
 *              // final def provideMyService(
 *              //   injector: Injector,
 *              //   statsReceiver: StatsReceiver
 *              // ): Service[Request, Response] =
 *              //     myCoolFilter.andThen(newService(injector, statsReceiver))
 *              //
 *              // Or provide the `com.twitter.finatra.httpclient.HttpClient`
 *              //
 *              // ex:
 *              // @Provides
 *              // @Singleton
 *              // final def provideMyHttpClient(
 *              //   injector: Injector,
 *              //   statsReceiver: StatsReceiver,
 *              //   mapper: ScalaObjectMapper
 *              // ): HttpClient = newHttpClient(injector, statsReceiver, mapper)
 *
 *            }
 *          }}}
 *
 * @note This trait does not define any `@Provide` annotations or bindings.
 */
trait HttpClientModuleTrait extends StackClientModuleTrait[Request, Response, Http.Client] {

  // override and set to a non-empty value if the dest requires a Host header
  def hostname: String = ""

  def retryPolicy: Option[RetryPolicy[Try[Response]]] = None

  def defaultHeaders: Map[String, String] = Map.empty

  override protected final def baseClient: Http.Client = Http.Client()

  // Java friendly: https://issues.scala-lang.org/browse/SI-8905
  override final def newClient(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): Http.Client = super.newClient(injector, statsReceiver)

  final def newHttpClient(
    injector: Injector,
    statsReceiver: StatsReceiver,
    mapper: ScalaObjectMapper
  ): HttpClient = new HttpClient(
    hostname = hostname,
    httpService = newService(injector, statsReceiver),
    retryPolicy = retryPolicy,
    defaultHeaders = defaultHeaders,
    mapper = mapper
  )

}
