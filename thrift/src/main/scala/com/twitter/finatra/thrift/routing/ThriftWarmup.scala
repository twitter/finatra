package com.twitter.finatra.thrift.routing

import com.twitter.inject.Logging
import com.twitter.inject.thrift.utils.ThriftMethodUtils._
import com.twitter.scrooge.{Request, Response, ThriftMethod}
import com.twitter.util.{Await, Try}
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
   * @param responseCallback - callback called for every response where assertions can be made.
   * @tparam M - type of the [[com.twitter.scrooge.ThriftMethod]]
   * @note be aware that in the response callback, failed assertions that throw Exceptions could
   *       prevent a server from  restarting. This is generally when dependent services are
   *       unresponsive causing the warm-up request(s) to fail. As such, you should wrap your
   *       warm-up calls in these situations in a try/catch {}.
   */
  @deprecated("Use Request/Response based functionality", "2018-12-20")
  def send[M <: ThriftMethod](method: M, args: M#Args, times: Int = 1)(
    responseCallback: Try[M#SuccessType] => Unit = unitFunction
  ): Unit = {
    if (!router.isConfigured) throw new IllegalStateException("Thrift warmup requires a properly configured router")
    sendRequest(method, Request[M#Args](args), times) { response =>
      responseCallback(response.map(_.value))
    }
  }

  /**
   * Send a request to warmup services that are not yet externally receiving traffic.
   *
   * @param method - [[com.twitter.scrooge.ThriftMethod]] to send request through
   * @param req - [[com.twitter.scrooge.Request]] to send
   * @param times - number of times to send the request
   * @param responseCallback - callback called for every response where assertions can be made.
   * @tparam M - type of the [[com.twitter.scrooge.ThriftMethod]]
   * @note be aware that in the response callback, failed assertions that throw Exceptions could
   *       prevent a server from  restarting. This is generally when dependent services are
   *       unresponsive causing the warm-up request(s) to fail. As such, you should wrap your
   *       warm-up calls in these situations in a try/catch {}.
   */
  def sendRequest[M <: ThriftMethod](method: M, req: Request[M#Args], times: Int = 1)(
    responseCallback: Try[Response[M#SuccessType]] => Unit = unitFunction
  ): Unit = {
    if (!router.isConfigured) throw new IllegalStateException("Thrift warmup requires a properly configured router")
    val service = router.routeWarmup(method)
    for (_ <- 1 to times) {
      time(s"Warmup ${prettyStr(method)} completed in %sms.") {
        responseCallback(Await.result(service(req).liftToTry))
      }
    }
  }

  @deprecated("This is now a no-op.", "2018-03-20")
  def close(): Unit = {}
}
