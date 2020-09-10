package com.twitter.finatra.thrift.routing

import com.twitter.inject.Logging
import com.twitter.inject.thrift.utils.ThriftMethodUtils._
import com.twitter.scrooge.{Request, Response, ThriftMethod}
import com.twitter.util.{Await, Try}
import javax.inject.Inject

private object ThriftWarmup {

  /**  Function curried as the default arg for the responseCallback: M#SuccessType => Unit parameter. */
  val unitFunction: AnyRef => Unit = _ => ()
}

/**
 * A utility for performing requests to endpoints defined by a configured [[ThriftRouter]] for the
 * purpose of warming up the `ThriftServer`.
 *
 * @note This only provides routing to user-defined routes of the configured [[ThriftRouter]].
 * @note This is only for use with generated Scala code which uses the [[ThriftRouter]].
 *
 * @param router the configured [[com.twitter.finatra.thrift.routing.ThriftRouter]]
 *
 * @see [[com.twitter.finatra.thrift.routing.ThriftRouter]]
 */
class ThriftWarmup @Inject() (
  router: ThriftRouter)
    extends Logging {
  import ThriftWarmup._

  /* Public */

  /**
   * Send a request to warmup services that are not yet externally receiving traffic.
   *
   * @param method the[[com.twitter.scrooge.ThriftMethod]] to request
   * @param args the [[com.twitter.scrooge.ThriftMethod]].Args to send
   * @param times the number of times to send the request
   * @param responseCallback a callback called for every response where assertions can be made.
   * @tparam M the type of the [[com.twitter.scrooge.ThriftMethod]]
   *
   * @note be aware that in the response callback, failed assertions that throw exceptions could
   *       prevent a server from starting. This is generally when dependent services are
   *       unresponsive, causing the warm-up request(s) to fail. As such, you should wrap your
   *       warm-up calls in these situations in a try/catch {}.
   */
  @deprecated("Use Request/Response based functionality", "2018-12-20")
  def send[M <: ThriftMethod](
    method: M,
    args: M#Args,
    times: Int = 1
  )(
    responseCallback: Try[M#SuccessType] => Unit = unitFunction
  ): Unit = {
    if (!router.isConfigured)
      throw new IllegalStateException("Thrift warmup requires a properly configured router")
    sendRequest(method, Request[M#Args](args), times) { response =>
      responseCallback(response.map(_.value))
    }
  }

  /**
   * Send a request to warmup services that are not yet externally receiving traffic.
   *
   * @param method the [[com.twitter.scrooge.ThriftMethod]] to request
   * @param req the [[com.twitter.scrooge.Request]] to send
   * @param times the number of times to send the request
   * @param responseCallback a callback called for every response where assertions can be made.
   * @tparam M the type of the [[com.twitter.scrooge.ThriftMethod]]
   *
   * @note be aware that in the response callback, failed assertions that throw Exceptions could
   *       prevent a server from  restarting. This is generally when dependent services are
   *       unresponsive causing the warm-up request(s) to fail. As such, you should wrap your
   *       warm-up calls in these situations in a try/catch {}.
   *
   * @see [[http://twitter.github.io/finatra/user-guide/thrift/controllers.html]]
   */
  def sendRequest[M <: ThriftMethod](
    method: M,
    req: Request[M#Args],
    times: Int = 1
  )(
    responseCallback: Try[Response[M#SuccessType]] => Unit = unitFunction
  ): Unit = {
    if (!router.isConfigured)
      throw new IllegalStateException("Thrift warmup requires a properly configured router")
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
