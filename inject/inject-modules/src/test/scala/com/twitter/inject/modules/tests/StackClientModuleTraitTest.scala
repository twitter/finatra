package com.twitter.inject.modules.tests

import com.google.inject.{Guice, Provides}
import com.twitter.finagle.{ClientConnection, Service, ServiceFactory, Stack, Stackable}
import com.twitter.finagle.client.{EndpointerModule, EndpointerStackClient, StackClient}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.{Injector, InjectorModule, Test, TwitterModule}
import com.twitter.inject.modules.{StackClientModuleTrait, StatsReceiverModule}
import com.twitter.util.{Future, Time}
import java.net.SocketAddress
import javax.inject.Singleton

class StackClientModuleTraitTest extends Test {
  import StackClientModuleTraitTest._

  test("can create a module for a new stack client") {
    val injector = Guice.createInjector(TestModule, InjectorModule, StatsReceiverModule)
    injector.getInstance(Test.Client.getClass) should not be null
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
      extends EndpointerStackClient[Request, Response, Client] {

      override protected def endpointer: Stackable[ServiceFactory[Request, Response]] =
        new EndpointerModule[Request, Response](
          Seq.empty, { (_: Stack.Params, _: SocketAddress) =>
            new ServiceFactory[Request, Response] {
              def apply(conn: ClientConnection): Future[Service[Request, Response]] =
                Future.value(Service.mk[Request, Response](_ => Future.value(Response())))
              def close(deadline: Time): Future[Unit] = Future.Done
            }
          }
        )

      override protected def copy1(
        stack: Stack[ServiceFactory[Request, Response]],
        params: Stack.Params
      ): Test.Client = this.copy(stack = stack, params = params)

    }

    def client: Test.Client = Client()
  }

  object TestModule extends TestModule

  // A module that allows us to provide a concrete `Test.Client` for testing
  class TestModule
    extends TwitterModule
      with StackClientModuleTrait[Request, Response, Test.Client] {
    def label: String = "test-client"
    def dest: String = "/s/service/a"
    protected def baseClient: Test.Client = Test.client

    @Provides
    @Singleton
    def providesTestClient(injector: Injector, statsReceiver: StatsReceiver): Test.Client =
      newClient(injector, statsReceiver)
  }

}
