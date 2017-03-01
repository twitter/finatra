package com.twitter.finatra.thrift.routing

import com.twitter.finatra.thrift.internal.ThriftMethodService
import com.twitter.finatra.utils.FuturePools
import com.twitter.inject.Logging
import com.twitter.inject.thrift.utils.ThriftMethodUtils._
import com.twitter.scrooge.ThriftMethod
import com.twitter.util.Await
import javax.inject.Inject

class ThriftWarmup @Inject()(
  router: ThriftRouter)
  extends Logging {

  /* Use a FuturePool to avoid getting a ConstFuture from Future.apply(...) */
  private val pool = FuturePools.fixedPool("Thrift Warmup", 1)

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
  def send[M <: ThriftMethod](
    method: M,
    args: M#Args,
    times: Int = 1)(responseCallback: M#Result => Unit = unitFunction _): Unit = {

    for (i <- 1 to times) {
      time(s"Warmup ${prettyStr(method)} completed in %sms.") {
        val response = executeRequest(method, args)
        responseCallback(response)
      }
    }
  }

  def close() = {
    pool.executor.shutdownNow()
  }

  /* Private */

  private def executeRequest[M <: ThriftMethod](
    method: M,
    args: M#Args): M#Result = {

    val response = pool {
      info(s"Warmup ${prettyStr(method)}")
      val service = router.methods(method).asInstanceOf[ThriftMethodService[M#Args, M#Result]]
      service(args)
    }.flatten

    Await.result(response)
  }

  /**
    * Function curried as the default arg for the responseCallback: M#Result => Unit parameter.
 *
    * @param a - [[AnyRef]]
    * @return Unit
    */
  private def unitFunction(a: AnyRef): Unit = ()
}
