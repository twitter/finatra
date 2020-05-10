package com.twitter.inject.requestscope

import com.twitter.finagle.{Filter, Service}
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, IntegrationTest, TwitterModule}
import com.twitter.util.Future
import javax.inject.{Inject, Provider}

class FinagleRequestScopeFilterIntegrationTest extends IntegrationTest {

  override val injector: Injector =
    TestInjector(
      FinagleRequestScopeModule,
      MyTestRequestScopeModule
    ).create

  test("request scoped filter") {
    val myTestRequestScopeService = injector.instance[MyTestRequestScopeService]

    val finagleRequestScopeFilter = injector.instance[FinagleRequestScopeFilter[String, String]]
    val testUserRequestScopeFilter = injector.instance[TestUserRequestScopeFilter[String, String]]

    val filteredService = finagleRequestScopeFilter
      .andThen(testUserRequestScopeFilter).andThen(myTestRequestScopeService)
    await(filteredService.apply("hi")) should equal("Bob said hi")
  }

  test("request scoped type agnostic filter") {
    val myTestRequestScopeService = injector.instance[MyTestRequestScopeService]

    val finagleRequestScopeFilter = injector.instance[FinagleRequestScopeFilter.TypeAgnostic]
    val testUserRequestScopeFilter = injector.instance[TypeAgnosticTestUserRequestScopeFilter]

    val filteredService = finagleRequestScopeFilter
      .andThen(testUserRequestScopeFilter).andThen(myTestRequestScopeService)
    await(filteredService.apply("hi")) should equal("Bob said hi")
  }
}

object MyTestRequestScopeModule extends TwitterModule with RequestScopeBinding {
  override protected def configure(): Unit = {
    bindRequestScope[TestUser]
  }
}

case class TestUser(name: String)

class TestUserRequestScopeFilter[Req, Rep] @Inject() (requestScope: FinagleRequestScope)
    extends Filter[Req, Rep, Req, Rep] {
  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    requestScope.seed[TestUser](TestUser("Bob"))
    service(request)
  }
}

class TypeAgnosticTestUserRequestScopeFilter @Inject() (requestScope: FinagleRequestScope)
    extends Filter.TypeAgnostic {
  def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = new TestUserRequestScopeFilter(requestScope)
}

class MyTestRequestScopeService @Inject() (testUserProvider: Provider[TestUser])
    extends Service[String, String] {

  override def apply(request: String): Future[String] = {
    Future(testUserProvider.get.name + " said " + request)
  }
}
