package com.twitter.finatra.http.tests.integration.requestscope

import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.http.filters.ExceptionMappingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpServer}
import com.twitter.finatra.utils.FuturePools
import com.twitter.finatra.utils.RetryPolicyUtils.constantRetry
import com.twitter.finatra.utils.RetryUtils.retry
import com.twitter.inject.TwitterModule
import com.twitter.inject.requestscope.{FinagleRequestScope, FinagleRequestScopeFilter, RequestScopeBinding}
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Future, Return, Try}
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.{Inject, Provider}
import scala.collection.JavaConversions._

class RequestScopeFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new PooledServer)

  "request scope propagates to multiple future pools" in {
    for (i <- 1 to 50) {
      server.httpGet(
        "/hi?msg=hello",
        headers = Map("Username" -> "Bob"),
        andExpect = Ok,
        withBody = "Hello Bob who said hello")

      server.httpGet(
        "/hi?msg=yo",
        headers = Map("Username" -> "Sally"),
        andExpect = Ok,
        withBody = "Hello Sally who said yo")

      val expectedMsgs = Seq(
        "User Bob said hello",
        "User Sally said yo",
        "Pool1 User Bob said hello",
        "Pool1 User Sally said yo",
        "Pool2 User Bob said hello",
        "Pool2 User Sally said yo").sorted

      retry(constantRetry[Boolean](
        start = 1.second,
        numRetries = 200,
        shouldRetry = {case Return(expectedMatches) => !expectedMatches})) {

        FuturePooledController.msgLog.toSeq.sorted == expectedMsgs
      } should be(Try(true))

      FuturePooledController.msgLog.clear()
    }

    FuturePooledController.pool1.executor.shutdown()
    FuturePooledController.pool2.executor.shutdown()
  }
}


/* ==================================================== */
/* Request Scope Filter */
class TestUserRequestScopeFilter @Inject()(
  requestScope: FinagleRequestScope)
  extends SimpleFilter[Request, Response] {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    val username = request.headerMap.get("Username").get
    requestScope.seed[TestUser](TestUser(username))
    service(request)
  }
}

/* ==================================================== */
/* Request Scope Filter Module */
object TestUserRequestScopeFilterModule extends TwitterModule with RequestScopeBinding {
  override protected def configure() {
    bindRequestScope[TestUser]
  }
}

/* ==================================================== */
/* Request Scoped Class */
case class TestUser(name: String)

/* ==================================================== */
/* Controller Accessing Request Scope */
object FuturePooledController {
  val msgLog = new ConcurrentLinkedQueue[String]
  val pool1 = FuturePools.unboundedPool("FuturePooledController 1")
  val pool2 = FuturePools.unboundedPool("FuturePooledController 2")
}

class FuturePooledController @Inject()(
  testUserProvider: Provider[TestUser])
  extends Controller {

  get("/hi") { request: Request =>
    val msg = request.params("msg")
    FuturePooledController.msgLog.add("User " + testUserProvider.get().name + " said " + msg)
    info(msg)

    FuturePooledController.pool1 {
      val msg2 = "Pool1 User " + testUserProvider.get().name + " said " + msg
      info(msg2)
      FuturePooledController.msgLog.add(msg2)
      FuturePooledController.pool2 {
        val msg3 = "Pool2 User " + testUserProvider.get().name + " said " + msg
        info(msg3)
        FuturePooledController.msgLog.add(msg3)
      }
    }

    response.ok.body("Hello " + testUserProvider.get().name + " who said " + msg)
  }
}

/* ==================================================== */
/* Server */
class PooledServer extends HttpServer {
  override def modules = Seq(TestUserRequestScopeFilterModule)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[ExceptionMappingFilter[Request]]
      .filter[FinagleRequestScopeFilter[Request, Response]]
      .filter[TestUserRequestScopeFilter]
      .add[FuturePooledController]
  }
}
