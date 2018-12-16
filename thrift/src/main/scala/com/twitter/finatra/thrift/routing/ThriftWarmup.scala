package com.twitter.finatra.thrift.routing

import com.twitter.finatra.thrift.internal.ThriftMethodService
import com.twitter.inject.Logging
import com.twitter.inject.thrift.utils.ThriftMethodUtils._
import com.twitter.scrooge.ThriftMethod
import com.twitter.util.{Await, Future, Try}
import javax.inject.Inject

private object ThriftWarmup {
  /**  Function curried as the default arg for the responseCallback: M#SuccessType => Unit parameter. */
  val unitFunction: AnyRef => Unit = _ => Unit
}

/**
 * A utility for performing requests through a configured [[ThriftRouter]] for the purpose of
 * warming up the `ThriftServer`.
 *
 * @note This is only for use with generated Scala code which uses the [[ThriftRouter]].
 *
 * @param router the configured [[com.twitter.finatra.thrift.routing.ThriftRouter]]
 */
class ThriftWarmup @Inject()(
  router: ThriftRouter
) extends Logging {
  import ThriftWarmup._

  /* Public */

  /**
   * Send a request to warmup services that are not yet externally receiving traffic.
   *
   * @param method - [[com.twitter.scrooge.ThriftMethod]] to send request through
   * @param args - [[com.twitter.scrooge.ThriftMethod]].Args to send
   * @param times - number of times to send the request
   * @param responseCallback - callback called for every response where assertions can be made. NOTE: be aware that
   *                         failed assertions that throws Exceptions could prevent a server from restarting. This is
   *                         generally when dependent services are unresponsive causing the warm-up request(s) to fail.
   *                         As such, you should wrap your warm-up calls in these situations in a try/catch {}.
   * @tparam M - type of the [[com.twitter.scrooge.ThriftMethod]]
   */
  def send[M <: ThriftMethod](method: M, args: M#Args, times: Int = 1)(
    responseCallback: Try[M#SuccessType] => Unit = unitFunction
  ): Unit = {
    if (!router.isConfigured) throw new IllegalStateException("Thrift warmup requires a properly configured router")
    for (_ <- 1 to times) {
      time(s"Warmup ${prettyStr(method)} completed in %sms.") {
        val response = executeRequest(method, args)
        responseCallback(response)
      }
    }
  }

  @deprecated("This is now a no-op.", "2018-03-20")
  def close(): Unit = {}

  /* Private */

  private def executeRequest[M <: ThriftMethod](method: M, args: M#Args): Try[M#SuccessType] = {
    Try(Await.result(routeRequest(method, args)))
  }

  private def routeRequest[M <: ThriftMethod](method: M, args: M#Args): Future[M#SuccessType] = {
    val service =
      router
        .thriftMethodService(method)
        .asInstanceOf[ThriftMethodService[M#Args, M#SuccessType]]
    service(args)
  }
}
