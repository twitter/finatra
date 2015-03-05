package com.twitter.finatra.integration.requestscope

import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.routing.Router
import com.twitter.finatra.test.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.utils.RetryPolicyUtils.constantRetry
import com.twitter.finatra.utils.RetryUtils.retry
import com.twitter.finatra.{Controller, FinatraServer}
import com.twitter.inject.TwitterModule
import com.twitter.inject.requestscope.{FinagleRequestScope, FinagleRequestScopeFilter, RequestScopeBinding}
import com.twitter.util.{Future, FuturePool, Return, Try}
import javax.inject.{Inject, Provider}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/* ==================================================== */
class RequestScopeIntegrationTest extends HttpTest {

  val server = new EmbeddedHttpServer(
    twitterServer = new PooledServer)

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

        FuturePooledController.msgLog.sorted == expectedMsgs
      } should be(Try(true))

      FuturePooledController.msgLog.clear()
    }
  }
}


/* ==================================================== */
/* Request Scope Filter */
class TestUserRequestScopeFilter @Inject()(
  requestScope: FinagleRequestScope)
  extends SimpleFilter[Request, Response] {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    val username = request.headers.get("Username")
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
  val msgLog = new ArrayBuffer[String] with mutable.SynchronizedBuffer[String]
}

class FuturePooledController @Inject()(
  testUserProvider: Provider[TestUser])
  extends Controller {

  private val pool1 = FuturePool.unboundedPool
  private val pool2 = FuturePool.unboundedPool

  get("/hi") { request: Request =>
    val msg = request.params("msg")
    FuturePooledController.msgLog += ("User " + testUserProvider.get().name + " said " + msg)
    info(msg)

    pool1 {
      val msg2 = "Pool1 User " + testUserProvider.get().name + " said " + msg
      info(msg2)
      FuturePooledController.msgLog += msg2
      pool2 {
        val msg3 = "Pool2 User " + testUserProvider.get().name + " said " + msg
        info(msg3)
        FuturePooledController.msgLog += msg3
      }
    }

    response.ok.body("Hello " + testUserProvider.get().name + " who said " + msg)
  }
}

/* ==================================================== */
/* Server */
class PooledServer extends FinatraServer {
  override def modules = Seq(TestUserRequestScopeFilterModule)

  override def configure(router: Router) {
    router.
      filter[CommonFilters].
      filter[FinagleRequestScopeFilter[Request, Response]].
      filter[TestUserRequestScopeFilter].
      add[FuturePooledController]
  }
}
