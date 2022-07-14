package com.twitter.inject.modules.tests

import com.google.inject.Guice
import com.google.inject.Provides
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.ClientConnection
import com.twitter.finagle.Service
import com.twitter.finagle.ServiceFactory
import com.twitter.finagle.Stack
import com.twitter.finagle.Stackable
import com.twitter.finagle.Status
import com.twitter.finagle.client.EndpointerModule
import com.twitter.finagle.client.EndpointerStackClient
import com.twitter.finagle.client.StackClient
import com.twitter.finagle.factory.TimeoutFactory
import com.twitter.finagle.param.Label
import com.twitter.finagle.param.Monitor
import com.twitter.finagle.param.Stats
import com.twitter.finagle.service.Retries
import com.twitter.finagle.service.RetryBudget
import com.twitter.finagle.service.TimeoutFilter
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Injector
import com.twitter.inject.InjectorModule
import com.twitter.inject.Test
import com.twitter.inject.TwitterModule
import com.twitter.inject.modules.StackClientModuleTrait
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.modules.TracerModule
import com.twitter.util.Closable
import com.twitter.util.Duration
import com.twitter.util.Future
import com.twitter.util.NullMonitor
import com.twitter.util.Time
import java.net.SocketAddress
import javax.inject.Singleton

class StackClientModuleTraitTest extends Test {
  import StackClientModuleTraitTest._

  test("can create a module for a new stack client") {
    val injector =
      Guice.createInjector(TestModule, InjectorModule, StatsReceiverModule, TracerModule)
    injector.getInstance(Test.Client.getClass) should not be null
  }

  // The following tests take a base/default module configuration and layer customization on
  // top, using the different extension methods. The configuration ordering is:
  // initialClientConfig -> configureClient -> frameworkConfigureClient

  test("verify client initial configuration") {
    val injector =
      Guice.createInjector(DefaultTestModule, InjectorModule, StatsReceiverModule, TracerModule)
    val clnt = injector.getInstance(classOf[Test.Client])
    assert(clnt.params[TimeoutFilter.Param].timeout == Duration.Top)
    assert(clnt.params[TimeoutFactory.Param].timeout == Duration.Top)
    assert(clnt.params[Stats].statsReceiver != null)
    assert(clnt.params[Monitor].monitor == NullMonitor)
    assert(clnt.params[Label].label == "test-client")
    assert(clnt.params[Retries.Budget].retryBudget.balance == RetryBudget().balance)
  }

  test("verify client initial configuration with parameter override") {
    val injector =
      Guice.createInjector(TestModule, InjectorModule, StatsReceiverModule, TracerModule)
    val clnt = injector.getInstance(classOf[Test.Client])
    assert(clnt.params[TimeoutFilter.Param].timeout == 200.milliseconds)
    assert(clnt.params[TimeoutFactory.Param].timeout == 1.second)
    assert(clnt.params[Stats].statsReceiver != null)
    assert(clnt.params[Monitor].monitor == com.twitter.util.RootMonitor)
    assert(clnt.params[Label].label == "test-client")
    assert(
      clnt.params[Retries.Budget].retryBudget.balance == RetryBudget(1.second, 100, .3).balance)
  }

  test("verify client custom configuration override") {
    val injector =
      Guice.createInjector(CustomizedTestModule, InjectorModule, StatsReceiverModule, TracerModule)
    val clnt = injector.getInstance(classOf[Test.Client])
    assert(clnt.params[TimeoutFilter.Param].timeout == 100.milliseconds)
    assert(clnt.params[TimeoutFactory.Param].timeout == 1.second)
    assert(clnt.params[Stats].statsReceiver != null)
    assert(clnt.params[Monitor].monitor == com.twitter.util.RootMonitor)
    assert(clnt.params[Label].label == "test-client")
    assert(
      clnt.params[Retries.Budget].retryBudget.balance == RetryBudget(1.second, 100, .3).balance)
  }

  test("verify framework client configuration override") {
    val injector =
      Guice.createInjector(FrameworkTestModule, InjectorModule, StatsReceiverModule, TracerModule)
    val clnt = injector.getInstance(classOf[Test.Client])
    assert(clnt.params[TimeoutFilter.Param].timeout == 50.milliseconds)
    assert(clnt.params[TimeoutFactory.Param].timeout == 1.second)
    assert(clnt.params[Stats].statsReceiver != null)
    assert(clnt.params[Monitor].monitor == com.twitter.util.RootMonitor)
    assert(clnt.params[Label].label == "test-client")
    assert(
      clnt.params[Retries.Budget].retryBudget.balance == RetryBudget(1.second, 100, .3).balance)
  }
}

// Test utilities
object StackClientModuleTraitTest {
  case class Request()
  case class Response()

  object Test {

    // EndpointerStackClient implementation for testing
    case class Client(
      stack: Stack[ServiceFactory[Request, Response]] = StackClient.newStack,
      params: Stack.Params = StackClient.defaultParams)
        extends EndpointerStackClient[Request, Response, Client]
        with Closable {

      override protected def endpointer: Stackable[ServiceFactory[Request, Response]] =
        new EndpointerModule[Request, Response](
          Seq.empty,
          { (_: Stack.Params, _: SocketAddress) =>
            new ServiceFactory[Request, Response] {
              def apply(conn: ClientConnection): Future[Service[Request, Response]] =
                Future.value(Service.mk[Request, Response](_ => Future.value(Response())))
              def close(deadline: Time): Future[Unit] = Future.Done
              def status: Status = Status.Open
            }
          }
        )

      override protected def copy1(
        stack: Stack[ServiceFactory[Request, Response]],
        params: Stack.Params
      ): Test.Client = this.copy(stack = stack, params = params)

      /**
       * Close the resource with the given deadline. This deadline is advisory,
       * giving the callee some leeway, for example to drain clients or finish
       * up other tasks.
       */
      override def close(deadline: Time): Future[Unit] = Future.Done
    }

    def client: Test.Client = Client()
  }

  object DefaultTestModule extends DefaultTestModule

  class DefaultTestModule
      extends TwitterModule
      with StackClientModuleTrait[Request, Response, Test.Client] {
    def label: String = "test-client"
    def dest: String = "/s/service/a"
    protected def baseClient: Test.Client = Test.client

    @Provides
    @Singleton
    final def providesTestClient(injector: Injector, statsReceiver: StatsReceiver): Test.Client =
      newClient(injector, statsReceiver)
  }

  object TestModule extends TestModule

  // A module that allows us to provide a concrete `Test.Client` for testing
  class TestModule extends DefaultTestModule {
    override protected def requestTimeout: Duration = 200.milliseconds
    override protected def sessionAcquisitionTimeout: Duration = 1.second
    override protected def retryBudget: RetryBudget = RetryBudget(1.second, 100, .3)
    override protected def monitor = com.twitter.util.RootMonitor
  }

  object CustomizedTestModule extends CustomizedTestModule

  // A module that allows us to provide a concrete `Test.Client` for testing
  class CustomizedTestModule extends TestModule {
    override protected def configureClient(injector: Injector, client: Test.Client): Test.Client =
      client.withRequestTimeout(100.milliseconds)
  }

  // A module that allows us to provide a concrete `Test.Client` for testing
  object FrameworkTestModule extends CustomizedTestModule {
    override protected[twitter] def frameworkConfigureClient(
      injector: Injector,
      client: Test.Client
    ): Test.Client =
      client.withRequestTimeout(50.milliseconds)
  }

}
