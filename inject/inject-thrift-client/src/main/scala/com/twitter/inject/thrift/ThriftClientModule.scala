package com.twitter.inject.thrift

import com.google.inject.Provides
import com.google.inject.matcher.Matchers
import com.twitter.finagle._
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.builder.ClientConfig.Yes
import com.twitter.finagle.stats.LoadedStatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.thrift.internal._
import com.twitter.inject.{TwitterPrivateModule, TypeUtils}
import com.twitter.scrooge._
import java.lang.reflect.Constructor
import javax.inject.Singleton
import org.joda.time.Duration
import scala.reflect.{ClassTag, _}

object ThriftClientModule {
  type ThriftClientFilter = Filter[FinatraThriftClientRequest, Any, FinatraThriftClientRequest, Any]
}

/**
 * Create a thrift client for the passed in Scrooge generated FutureIface
 *
 * RetryFilter is applied after Scrooge decodes the Thrift message, so both internal thrift and
 * IDL defined exceptions can be retried.
 *
 * Currently can only be used with Future type param version of Scrooge generated clients e.g. EchoService[Future]
 * due to Scala 2.10 incompatibilities between manifests and higher kinded types
 */
abstract class ThriftClientModule[T <: ThriftService : ClassTag]
  extends TwitterPrivateModule
  with RetryUtils {

  private val clientFilters = new ThriftClientFilters(
    module = this,
    statsReceiver = LoadedStatsReceiver)

  assert(!classTag[T].runtimeClass.getName.endsWith("FutureIface"), "FutureIface syntax not supported. Please use YourThriftService[Future] syntax")

  /* Overrides */

  override val modules = Seq(ThriftClientCommonModule)

  override def configure() {
    configureFilters(clientFilters)

    interceptThriftClientMethodCalls()

    val thriftClientKey = TypeUtils.createKeyWithFutureTypeParam[T]
    bind(thriftClientKey).toConstructor(finagledClientConstructor).in(classOf[Singleton])
    expose(thriftClientKey)
  }

  /* Protected */

  /**
   * Name of client for use in metrics
   */
  val label: String

  /**
   * Destination of client (usually a wily path)
   */
  val dest: String

  /**
   * ClientId to identify client calling the thrift service.
   * Note: clientId is a def so that it's value can come from a flag
   */
  def clientId: String = ""

  /**
   * Enable thrift mux for this connection.
   *
   * Note: Both server and client must have mux enabled otherwise
   * a non-descript ChannelClosedException will be seen.
   */
  val mux: Boolean = true

  def requestTimeout: Duration = MaxDuration

  def connectTimeout: Duration = MaxDuration

  type ConfiguredClientBuilder = ClientBuilder[thrift.ThriftClientRequest, Array[Byte], Yes, Yes, Yes]#This

  protected def configureClientBuilder(clientBuilder: ConfiguredClientBuilder): ConfiguredClientBuilder = {
    clientBuilder
  }

  protected def configureFilters(clientFilters: ThriftClientFilters): Unit = {

  }

  @Provides
  @Singleton
  def providesThriftService: Service[thrift.ThriftClientRequest, Array[Byte]] = {
    val client = if (mux)
      ThriftMux.Client().withClientId(ClientId(clientId))
    else
      Thrift.Client().withClientId(ClientId(clientId))

    configureClientBuilder(
      ClientBuilder()
        .stack(client)
        .dest(dest)
        .requestTimeout(requestTimeout.toTwitterDuration)
        .connectTimeout(connectTimeout.toTwitterDuration)).build()
  }

  @Provides
  @Singleton
  def providesThriftClientFilterChain(
    terminatingFilter: TerminatingThriftClientFilter): ThriftClientFilterChain = {

    /* TerminatingThriftClientFilter is always the last filter in the client filter chain,
     * which is used to support record/replay functionality in feature tests.
     * TODO: Refactor to remove the need for this special case filter
     */
    clientFilters.filter(terminatingFilter.filter)

    clientFilters.createThriftClientFilterChain
  }

  /* Private */

  // Only intercept client calls if filters have been added
  private def interceptThriftClientMethodCalls(): Unit = {
    if (clientFilters.filtersAdded) {
      val thriftClientMethodInterceptor = new ThriftClientMethodInterceptor(
        label = label,
        service = new ThriftClientRequestExecuteService,
        thriftClientFilterChainProvider = getProvider[ThriftClientFilterChain])

      bindInterceptor(
        Matchers.subclassesOf(classOf[ThriftService]),
        PublicFutureMethodMatcher,
        thriftClientMethodInterceptor)
    }
  }

  private def finagledClientConstructor: Constructor[T] = {
    val clientClassName = classTag[T].runtimeClass.getName
    val finagleClientClassName = clientClassName + "$FinagleClient"
    val finagleClientClass = Class.forName(finagleClientClassName)
    finagleClientClass.getConstructors.head.asInstanceOf[Constructor[T]]
  }
}
