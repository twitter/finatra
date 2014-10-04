package com.twitter.finatra.requestscope

import com.twitter.finatra.conversions.time._
import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.guice.GuiceModule
import com.twitter.finatra.test.{EmbeddedTwitterServer, HttpTest}
import com.twitter.finatra.twitterserver.routing.Router
import com.twitter.finatra.utils.{RetryUtils, RetryPolicyUtils}
import com.twitter.finatra.{Controller, FinatraServer}
import com.twitter.util.{Try, Return, Future, FuturePool}
import javax.inject.{Inject, Provider}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class RequestScopeIntegrationTest extends HttpTest {

  val server = new EmbeddedTwitterServer(
    twitterServer = new PooledServer)

  "request scope propagates to multiple future pools" in {
    server.httpGet(
      "/hi?msg=hello",
      headers = Map("Username" -> "Bob"),
      andExpect = Ok,
      withBody = "Hello Bob")

    server.httpGet(
      "/hi?msg=yo",
      headers = Map("Username" -> "Sally"),
      andExpect = Ok,
      withBody = "Hello Sally")

    val expectedMsgs = Seq(
      "User Bob said hello",
      "User Sally said yo",
      "Pool1 User Bob said hello",
      "Pool1 User Sally said yo",
      "Pool2 User Bob said hello",
      "Pool2 User Sally said yo")

    val expectedMsgsFound = RetryUtils.retry(RetryPolicyUtils.constantRetry[Boolean](
      start = 1.second,
      numRetries = 20,
      shouldRetry = {case Return(started) => !started})) {

      FuturePooledController.msgLog == expectedMsgs
    }

    expectedMsgsFound should be(Try(true))
  }
}


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

/* Request Scope Filter Module */
object TestUserRequestScopeFilterModule extends GuiceModule with RequestScopeBinding {
  override protected def configure() {
    bindRequestScope[TestUser]
  }
}

/* Request Scoped Class */
case class TestUser(name: String)

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
      Thread.sleep(1000)
      val msg2 = "Pool1 User " + testUserProvider.get().name + " said " + msg
      info(msg2)
      FuturePooledController.msgLog += msg2
      pool2 {
        Thread.sleep(1000)
        val msg3 = "Pool2 User " + testUserProvider.get().name + " said " + msg
        info(msg3)
        FuturePooledController.msgLog += msg3
      }
    }

    response.ok.body("Hello " + testUserProvider.get().name)
  }
}

/* Server */
class PooledServer extends FinatraServer {
  override def modules = Seq(TestUserRequestScopeFilterModule)

  override def configure(router: Router) {
    router.
      filter[CommonFilters].
      filter[FinagleRequestScopeFilter].
      filter[TestUserRequestScopeFilter].
      add[FuturePooledController]
  }
}
