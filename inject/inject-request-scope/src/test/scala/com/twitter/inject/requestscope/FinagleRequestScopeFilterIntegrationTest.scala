package com.twitter.inject.requestscope

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Test, TwitterModule}
import com.twitter.util.{Await, Future}
import javax.inject.{Provider, Inject}

class FinagleRequestScopeFilterIntegrationTest extends Test {

  val injector =
    TestInjector(FinagleRequestScopeModule, MyTestRequestScopeModule).create

  test("request scoped filter") {
    val finagleRequestScopeFilter = injector.instance[FinagleRequestScopeFilter[String, String]]
    val testUserRequestScopeFilter = injector.instance[TestUserRequestScopeFilter]
    val myTestRequestScopeService = injector.instance[MyTestRequestScopeService]
    val filteredService = finagleRequestScopeFilter andThen testUserRequestScopeFilter andThen myTestRequestScopeService

    Await.result(filteredService.apply("hi")) should equal("Bob said hi")
  }
}

object MyTestRequestScopeModule extends TwitterModule with RequestScopeBinding {
  override protected def configure() {
    bindRequestScope[TestUser]
  }
}

case class TestUser(name: String)

class TestUserRequestScopeFilter @Inject()(requestScope: FinagleRequestScope)
    extends SimpleFilter[String, String] {

  override def apply(request: String, service: Service[String, String]): Future[String] = {
    requestScope.seed[TestUser](TestUser("Bob"))
    service(request)
  }
}

class MyTestRequestScopeService @Inject()(testUserProvider: Provider[TestUser])
    extends Service[String, String] {

  override def apply(request: String): Future[String] = {
    Future(testUserProvider.get.name + " said " + request)
  }
}
