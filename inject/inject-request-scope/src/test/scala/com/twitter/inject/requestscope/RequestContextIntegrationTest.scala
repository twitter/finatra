package com.twitter.inject.requestscope

import com.google.inject.{Inject, Provider}
import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.routing.Router
import com.twitter.finatra.test.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.{Controller, FinatraServer}
import com.twitter.util.Future

object RequestContextIntegrationTest {
  val ageField = Request.Schema.newField[Int]()
  val lastNameField = Request.Schema.newField[LastName]()
}

class LastName(val name: String)


/* ==================================================== */
/* Test */
class RequestContextIntegrationTest extends HttpTest {

  val server = new EmbeddedHttpServer(
    twitterServer = new RequestContextServer
  )

  "context values are provided" in {
    server.httpGet(
      "/test",
      andExpect = Ok,
      withJsonBody = """{"age": 28, "last_name": "Leong"}"""
    )
  }
}

/* ==================================================== */
/* Filter */
class SetContextValuesFilter extends SimpleFilter[Request, Response] {
  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    request.ctx.update(RequestContextIntegrationTest.ageField, 28)
    request.ctx.update(RequestContextIntegrationTest.lastNameField, new LastName("Leong"))
    service(request)
  }
}

/* ==================================================== */
/* Module */
object ContextValuesModule extends RequestContextBinding {
  override def configure() = {
    bindRequestContext[Int, Age](RequestContextIntegrationTest.ageField)
    bindRequestContext[LastName](RequestContextIntegrationTest.lastNameField)
  }
}

/* ==================================================== */
/* Controller */
class CheckContextValuesController @Inject()(
  @Age ageProvider: Provider[Integer],
  lastNameProvider: Provider[LastName])
  extends Controller {

  get("/test") { request: Request =>
    response.ok.json(Map(
      "age" -> ageProvider.get,
      "last_name" -> lastNameProvider.get.name))
  }
}

/* ==================================================== */
/* Server */
class RequestContextServer extends FinatraServer {
  override def modules = Seq(ContextValuesModule)

  override def configure(router: Router) {
    router.
      filter[CommonFilters].
      filter[FinagleRequestScopeFilter[Request, Response]].
      filter[RequestSchemaRecordFilter].
      filter[SetContextValuesFilter].
      add[CheckContextValuesController]
  }
}