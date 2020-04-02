package com.twitter.finatra.http.tests.integration.json
import com.google.inject.Module
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpServer}
import com.twitter.finatra.validation.{ValidationException, Validator}
import com.twitter.inject.server.FeatureTest
import javax.inject.Inject

class CustomizedValidatorIntegrationServerFeatureTest extends FeatureTest {

  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    twitterServer = new HttpServer {
      override val name = "validation-server"
      override def modules: Seq[Module] = Seq(CustomizedValidatorModule)

      override protected def configureHttp(router: HttpRouter): Unit = {
        router.add[ValidationController]
      }
    },
    disableTestLogging = true
  )

  test("server should use the user injected Validator") {
    server.httpGet(
      "/validate_things",
      andExpect = Ok,
      withBody = "\nValidation Errors:\t\t" + "Whatever you provided is wrong." + "\n\n"
    )
  }
}

class ValidationController @Inject()(validator: Validator) extends Controller {
  get("/validate_things") { _: Request =>
    var errorMessage: String = ""
    try { validator.validate(Things(Seq.empty[String])) } catch {
      case e: ValidationException =>
        errorMessage = e.getMessage
    }
    errorMessage
  }
}
