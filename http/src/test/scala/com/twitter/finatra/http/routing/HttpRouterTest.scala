package com.twitter.finatra.http.routing

import com.google.inject.{Injector => GuiceInjector, AbstractModule, Binder, Module}
import com.twitter.finagle.httpx.{Response, Request, Method}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.internal.exceptions.ExceptionManager
import com.twitter.finatra.http.internal.marshalling.{CallbackConverter, MessageBodyManager}
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, Test}
import com.twitter.util.Future
import org.scalatest.OptionValues
import org.scalatest.mock.MockitoSugar
import net.codingwell.scalaguice.typeLiteral

class HttpRouterTest extends Test with OptionValues with MockitoSugar {

  "creates sub route" in {
    val router = createRouter()
    val mainRouter = router.add("/foo", router.createSubRouter.add(SubController))
    mainRouter.routes.exists(_.path == "/foo/bar") shouldBe true
  }

  "creates sub route from injector" in {
    val injector = TestInjector(createRouterModule[CustomRouter])
    createRouter(injector).routes.exists(_.path == "/bar/bar") shouldBe true
  }

  def createRouter(injector: Injector = Injector(mock[GuiceInjector])) = {
    new HttpRouter(injector, mock[CallbackConverter], mock[MessageBodyManager], mock[ExceptionManager])
  }

  def createRouterModule[T <: RoutesProvider: Manifest] = {
    new AbstractModule {
      override def configure(): Unit = {
        bind(classOf[RoutesProvider]).to(typeLiteral[T].getRawType.asInstanceOf[Class[T]])
      }
    }
  }
}

object SubController extends Controller {
  get("/bar") {
    "baz"
  }
}

class CustomRouter extends RoutesProvider {
  override def routes: Seq[Route] = createRoute(Method.Get, "/bar/bar") :: Nil

  def defaultCallback(request: Request) = {
    Future(Response())
  }

  def createRoute(method: Method, path: String): Route = {
    Route(
      name = "my_endpoint",
      method = method,
      path = path,
      callback = defaultCallback,
      annotations = Seq(),
      requestClass = classOf[Request],
      responseClass = classOf[Response])
  }
}