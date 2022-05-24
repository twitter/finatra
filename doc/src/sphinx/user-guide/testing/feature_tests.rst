.. _feature_tests:

Feature Tests
=============

.. important::

  Please see the section on including test-jar dependencies in your project: `Test Dependencies <../..#test-dependencies>`_.

.. tip::

    If you are calling an |c.t.util.Await|_ function on a |c.t.util.Future|_ return type in a
    test, it is generally considered good practice to ensure that your |c.t.util.Await|_ call
    includes a timeout duration, otherwise you may inadvertently cause your test to hang indefinitely
    if the awaited |c.t.util.Future|_ never completes.

    The base `c.t.inject.TestMixin <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/TestMixin.scala>`__ exposes an `#await <https://github.com/twitter/finatra/blob/ea0b7a6655f4a8df84b5933c0ade19cae311c098/inject/inject-core/src/test/scala/com/twitter/inject/TestMixin.scala#L103>`__ function which will call |c.t.util.Await#result|_
    with a test-defined `default timeout <https://github.com/twitter/finatra/blob/ea0b7a6655f4a8df84b5933c0ade19cae311c098/inject/inject-core/src/test/scala/com/twitter/inject/TestMixin.scala#L75>`__. This can be used in place of calling |c.t.util.Await#result|_ directly.

If you are familiar with `Gherkin <https://docs.behat.org/en/v2.5/guides/1.gherkin.html>`__ or
`Cucumber <https://github.com/cucumber/cucumber/wiki/Feature-Introduction>`__ or other similar
testing languages and frameworks, then `Feature Testing <https://wiki.documentfoundation.org/QA/Testing/Feature_Tests>`__
will feel somewhat familiar. In Finatra, a `FeatureTest` always consists of a **single** configured
server under test. See the |c.t.inject.server.FeatureTest|_ trait.

.. caution::

    The `server` is specified as a `def` in the |c.t.inject.server.FeatureTestMixin|_ trait.

    If you only want to start **one instance** of your server per test file, make sure to override this
    `def` with a `val`. See: `Sharing a Server Fixture Between Many Feature Tests <#sharing-a-server-fixture-between-many-feature-tests>`__
    for information on how to properly share a server test fixture.

We highly recommend writing `FeatureTests` for your services as they provide a very good signal of
whether you have correctly implemented the features of your service. As always, it is important to
always ask yourself, "*what are we trying to test?*" and to do what makes sense for your team but
we recommend including `FeatureTests` in your test suite.

.. admonition:: TL;DR

    A test which implements the `FeatureTest` trait is for a *single configured instance of a server under test*.
    Servers are not cheap to create and start, thus the typical pattern is to create (and usually start)
    the server once *before* any test case runs. The `FeatureTest` trait will ensure that the instance
    set to the `server` member is properly closed after all tests have been run.

    In short, the workflow of a `FeatureTest` looks like this:

    `instantiate test class --> create/start server --> run tests --> close/stop server`

With the above in mind, it is possible to multiple servers in a test or test different configurations
of a server.

Testing Multiple Applications or Servers
----------------------------------------

For multiple servers, don't use the |c.t.inject.server.FeatureTest|_ trait since it is for testing a
single server. Just extend the `c.t.inject.Test` trait to implement your test using the `embedded utilities <embedded.html>`_
to create and start your application. Note you will need to manually ensure to close any
created servers.

Many of the basics of feature testing mentioned here will still apply so it is still useful to read
through this documentation, just note again that you will need to ensure you manually close any created
servers to prevent any resource leaking in your tests.

Please take a look at these tests for examples of testing multiple embedded servers in a single test
file:

- `Http to Http example <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/tests/integration/multiserver/test/MultiServerFeatureTest.scala>`_
- `Http to Http to Thrift example <https://github.com/twitter/finatra/blob/develop/inject-thrift-client-http-mapper/src/test/scala/com/twitter/finatra/multiserver/test/MultiServerFeatureTest.scala>`_
- `Thrift to Thrift (via the DarkTrafficFilter) example <https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/scala/com/twitter/inject/thrift/MultiServerDarkTrafficFeatureTest.scala>`_
- `Thrift to Thrift (via the DarkTrafficFilter) Java example <https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/java/com/twitter/inject/thrift/integration/MultiJavaServerDarkTrafficFeatureTest.java>`_

Testing Multiple Configurations of a Server
-------------------------------------------

To test different configurations of a server, create different test files. That is, a test file should
map to a specific server configuration under test and test all the features of the configuration set.

For example if you had a server parameter that could either be 'yellow' or 'green' and wanted to
execute a suite of test cases against both of those configurations, the recommendation would be to
create two test files: `YellowServerFeatureTest` and `GreenServerFeatureTest`. Each test would have
the server configured accordingly.

See below for guidelines on how to `share a server fixture between feature tests <#sharing-a-server-fixture-between-many-feature-tests>`__,
specifically the `Sharing Test Cases <#id2>`_ section.

|c.t.server.TwitterServer|_
---------------------------

Finatra’s |c.t.inject.server.FeatureTest|_ utility can be used for testing any extension of |c.t.server.TwitterServer|_. 
That is, you can start a locally running server and write tests that issue requests to it as long as the 
server extends from |c.t.server.TwitterServer|_ -- the server under test does not specifically have to 
be a server written using the Finatra framework.

.. note:: 

  Some advanced features (like the automatic injection of an `InMemoryStatsReceiver <https://github.com/twitter/util/blob/develop/util-stats/src/main/scala/com/twitter/finagle/stats/InMemoryStatsReceiver.scala>`__ for performing metrics assertions) 
  that require use of injection will not be available if you only extend |c.t.server.TwitterServer|_ but you will be able 
  to start the server and test it in a controlled environment.

  For more information on creating an "injectable" TwitterServer, |c.t.inject.server.TwitterServer|_ see the documentation 
  `here <../twitter-server/index.html>`__.

Here’s an example of writing a test that starts a |c.t.server.TwitterServer|_ and asserts that it reports itself “healthy”.

Given a simple |c.t.server.TwitterServer|_:

.. code:: scala

    import com.twitter.finagle.{Http, ListeningServer, Service}
    import com.twitter.finagle.http.{Status, Response, Request}
    import com.twitter.server.TwitterServer
    import com.twitter.util.{Await, Future}
     
    class MyTwitterServer extends TwitterServer {
      private[this] val httpPortFlag =
        flag(name = "http.port", default = ":8888", help = "External HTTP server port")
     
      private[this] def responseString: String = "Hello, world!"
      private[this] val service = Service.mk[Request, Response] { request =>
        val response =
          Response(request.version, Status.Ok)
        response.contentString = responseString
        Future.value(response)
      }

      /** Simple way to expose the bound port once the external listening server is started */
      @volatile private[this] var _httpExternalPort: Option[Int] = None
      def httpExternalPort: Option[Int] = this._httpExternalPort
     
      def main(): Unit = {
        val server: ListeningServer = Http.server
          .withLabel("http")
          .serve(httpPortFlag(), service)
        info(s"Serving on port ${httpPortFlag()}")
        info(s"Serving admin interface on port ${adminPort()}")
        onExit {
          Await.result(server.close())
        }
        this._httpExternalPort = Some(server.boundAddress.asInstanceOf[InetSocketAddress].getPort)
        Await.ready(server)
      }
    }

Writing the FeatureTest
~~~~~~~~~~~~~~~~~~~~~~~

First, extend the |c.t.inject.server.FeatureTest|_ trait. Then override the `server` definition 
with an instance of your |EmbeddedTwitterServer|_ which wraps your |c.t.server.TwitterServer|_ 
under test.

.. code:: scala

    import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest, PortUtils}
    import scala.collection.immutable.ListMap
    
    class MyTwitterServerFeatureTest extends FeatureTest {
      
      override protected val server =
        new EmbeddedTwitterServer(
          twitterServer = new MyTwitterServer,
          globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
          flags = Map(
            "http.port" -> PortUtils.ephemeralLoopback,
            "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil"
          )
        )
      
      test("MyTwitterServer#starts") {
        server.isHealthy should be(true)
      }
    }

|c.t.inject.server.TwitterServer|_
----------------------------------

For an "injectable" TwitterServer, |c.t.inject.server.TwitterServer|_ the test would look exactly the same as above
for `c.t.server.TwitterServer <#c-t-server-twitterserver>`__ but you'll be able to take advantage of the `Injector <../getting-started/dependency_injection.html>`__
for overriding bound implementations to help create powerful tests. 

See the next sections on :ref:`mocks`, :ref:`override_modules`, and :ref:`bind_dsl` for details.

Testing With `Global Flags`
~~~~~~~~~~~~~~~~~~~~~~~~~~~

See the section covering this topic in the `Embedded Servers and Apps <embedded.html#testing-with-global-flags>`__
documentation.

Disabling Clients using `Dtabs`
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you have Finagle clients defined in your server which are using `Dtab <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Dtab.scala>`__ delegation tables for client resolution and want to 
keep them from making remote connections when your server starts, you can override the `Dtab` of the clients by 
passing the `-dtab.add` flag (defined by the `c.t.finagle.DtabFlags <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/DtabFlags.scala>`__ trait mixed into `c.t.server.TwitterServer <https://github.com/twitter/twitter-server/blob/55d6d28862f7f260f3171342ad8ca363553bac40/server/src/main/scala/com/twitter/server/TwitterServer.scala#L40>`__) 
to your server under test.

.. code:: scala

    import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest}
    import scala.collection.immutable.ListMap
 
    class MyTwitterServerFeatureTest extends FeatureTest {
     
      override protected val server =
        new EmbeddedTwitterServer(
          twitterServer = new MyTwitterServer,
          globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
          flags = Map(
            "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil")
        )
     
      test("MyTwitterServer#starts") {
        server.isHealthy should be(true)
      }
    }

Creating a Client to the Server Under Test
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

By default the |EmbeddedTwitterServer|_ will create a Finagle HTTP client to the 
`TwitterServer HTTP Admin interface <https://twitter.github.io/twitter-server/Admin.html>`__ 
accessible via `EmbeddedTwitterServer#httpAdminClient`. 

To get any bound *external* port of the server under test, you’ll need to structure your code to expose 
it for your test to be able to read once the server has been started. That is, the `c.t.finagle.ListeningServer <https://github.com/twitter/finagle/blob/cda049e7db679f62588eda1a18eadc846acb0b30/finagle-core/src/main/scala/com/twitter/finagle/Server.scala#L13>`__ 
started in your |c.t.server.TwitterServer|_ `main()` needs to be exposed such that you can call `server.boundAddress` 
after the server has been started.

Assuming we have exposed this bound port as written above with `MyTwitterServer#httpExternalPort` we could create
a client:

.. code:: scala

    import com.twitter.finagle.Http
    import com.twitter.finagle.http.{Request, Status}
    import com.twitter.finatra.http.request.RequestBuilder
    import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest}
    import java.net.InetAddress
    import scala.collection.immutable.ListMap
     
    class MyTwitterServerFeatureTest extends FeatureTest {

      private val testServer = new MyTwitterServer
     
      override protected val server =
        new EmbeddedTwitterServer(
          twitterServer = testServer,
          globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
          flags = Map(
            "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil")
        )
     
      private lazy val httpClient =
        Http.client
          .withSessionQualifier.noFailFast
          .withSessionQualifier.noFailureAccrual
          .newService(
            s"${InetAddress
                  .getLoopbackAddress
                  .getHostAddress
              }:${testServer.httpExternalPort.get}")
     
      override protected def beforeAll(): Unit = {
        server.start()
      }
     
      test("MyTwitterServer#starts") {
        server.isHealthy should be(true)
      }

      test("MyTwitterServer#feature") {
        val request = RequestBuilder.get("/foo")

        val response = await(httpClient(request))
        response.status should equal(Status.Ok)
      }
    }

This is where using the Finatra `c.t.finatra.http.HttpServer` or `c.t.finatra.thrift.ThriftServer` can help since much of the 
client creation work can then be done for you by the framework's testing tools, e.g., the |EmbeddedHttpServer|_ or
|EmbeddedThriftServer|_ without the need to add code to expose anything your external `ListeningServer`.

`c.t.finatra.http.HttpServer`
-----------------------------

To write a `FeatureTest` for an `c.t.finatra.http.HttpServer`, extend the |c.t.inject.server.FeatureTest|_
trait. Then override the `server` definition with an instance of your |EmbeddedHttpServer|_.

.. code:: scala

    import com.twitter.finagle.http.Status
    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.inject.server.FeatureTest
    import scala.collection.immutable.ListMap

    class ExampleServerFeatureTest extends FeatureTest {
      override val server = new EmbeddedHttpServer(
        twitterServer = new ExampleServer,
        globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
        flags = Map(
          "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil")
      )

      test("ExampleServer#perform feature") {
        server.httpGet(
          path = "/",
          andExpect = Status.Ok)
      }

      test("ExampleServer#perform another feature with a response") {
        val response = server.httpGet(
          path = "/foo",
          andExpect = Status.Ok)

        response.contentString should equal("Hello, world!")  
      }
    }

Note: The |EmbeddedHttpServer|_ creates a client to the external HTTP interface defined by the server and exposes
methods which use the client for issuing HTTP requests to the server under test.

You can also create a `c.t.finagle.http.Request` and execute it with the test HTTP client. Finatra has a simple `RequestBuilder <https://github.com/twitter/finatra/blob/develop/httpclient/src/main/scala/com/twitter/finatra/httpclient/RequestBuilder.scala>`__
to help easily construct a `c.t.finagle.http.Request`.

.. code:: scala

    import com.twitter.finagle.http.Status
    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.finatra.http.request.RequestBuilder
    import com.twitter.inject.server.FeatureTest
    import scala.collection.immutable.ListMap

    class ExampleServerFeatureTest extends FeatureTest {
      override val server = new EmbeddedHttpServer(
        twitterServer = new ExampleServer,
        globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
        flags = Map(
          "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil")
      )

      test("ExampleServer#perform feature") {
        server.httpGet(
          path = "/",
          andExpect = Status.Ok)
      }

      test("ExampleServer#perform feature with built request") {
        val request = RequestBuilder.get("/")

        val response = server.httpClient(request)
        response.status should equal(Status.Ok)
      }
    }

`c.t.finatra.thrift.ThriftServer`
---------------------------------

Similarly, to write a `FeatureTest` for a `c.t.finatra.thrift.ThriftServer` and create a `Finagle <https://twitter.github.io/finagle/>`__
`client <#thrift-tests>`__ to it, extend the |c.t.inject.server.FeatureTest|_ trait, override the
`server` definition with an instance of your |EmbeddedThriftServer|_, and then create a Thrift client
from the |EmbeddedThriftServer|_.

.. code:: scala

    import com.example.thriftscala.ExampleThrift
    import com.twitter.conversions.DurationOps._
    import com.twitter.finatra.thrift.EmbeddedThriftServer
    import com.twitter.inject.server.FeatureTest
    import com.twitter.util.Await
    import scala.collection.immutable.ListMap

    class ExampleThriftServerFeatureTest extends FeatureTest {
      override val server = new EmbeddedThriftServer(
        twitterServer = new ExampleThriftServer,
        globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
        flags = Map(
          "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil")
      )

      lazy val client: ExampleThrift.MethodPerEndpoint =
        server.thriftClient[ExampleThrift.MethodPerEndpoint](clientId = "client123")

      test("ExampleThriftServer#return data accordingly") {
        await(client.doExample("input")) should equal("output")
      }
    }

.. tip::

    Again, note that tests should **always** define a timeout for any |c.t.util.Await|_ call. We use the `#await <https://github.com/twitter/finatra/blob/ea0b7a6655f4a8df84b5933c0ade19cae311c098/inject/inject-core/src/test/scala/com/twitter/inject/TestMixin.scala#L103>`__ 
    function in the above example.

Thrift Client Interface Types
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As mentioned in the Scrooge `Finagle Integration <https://twitter.github.io/scrooge/Finagle.html>`__
documentation, users have three API choices for building an interface to a Finagle Thrift client —
``ServicePerEndpoint``, ``ReqRepServicePerEndpoint``, and ``MethodPerEndpoint``. This is true even
when creating a test Thrift client to a Thrift server.

In the example above, we create a Thrift client in the form of the higher-kinded type interface,
e.g., `MyService[+MM[_]]`. We could choose to create a `ExampleThrift.MethodPerEndpoint`
interface instead by changing the type parameter given to the |c.t.finatra.thrift.ThriftClient#thriftClient[T]|_
method:

.. code:: scala

    lazy val client: ExampleThrift.MethodPerEndpoint =
      server.thriftClient[ExampleThrift.MethodPerEndpoint](clientId = "client123")

Users can also choose to create a `service-per-endpoint` Thrift client interface by calling the
|c.t.finatra.thrift.ThriftClient#servicePerEndpoint[T]|_ with either the ``ServicePerEndpoint`` or
``ReqRepServicePerEndpoint`` type. E.g.,

.. code:: scala

    lazy val client: ExampleThrift.ServicePerEndpoint =
      server.servicePerEndpoint[ExampleThrift.ServicePerEndpoint](clientId = "client123")

or

.. code:: scala

    lazy val client: ExampleThrift.ReqRepServicePerEndpoint =
      server.servicePerEndpoint[ExampleThrift.ReqRepServicePerEndpoint](clientId = "client123")

Lastly, the Thrift client can also be expressed as a ``MethodPerEndpoint`` wrapping a
`service-per-endpoint` type by using |c.t.finatra.thrift.ThriftClient#methodPerEndpoint[T, U]|_.
This would allow for applying a set of filters on the Thrift client interface before interacting
with the Thrift client as a ``MethodPerEndpoint`` interface.

For example:

.. code:: scala

    lazy val servicePerEndpoint: ExampleThrift.ServicePerEndpoint =
      server
        .servicePerEndpoint[ExampleThrift.ServicePerEndpoint](clientId = "client123")
        .filtered(???)

    lazy val client: ExampleThrift.MethodPerEndpoint =
      server.methodPerEndpoint[
        ExampleThrift.ServicePerEndpoint,
        ExampleThrift.MethodPerEndpoint](servicePerEndpoint)

See the `Communicate with a Thrift Service <../thrift/clients.html>`__ section for more information
on Thrift clients.

Closing the Test Client Interface
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is considered a best practice to close any created test Thrift client interface to ensure that any
opened resources are closed.

For instance, if you are instantiating a single Thrift client interface for all of your tests, you
could close the client in the ScalaTest `afterAll` lifecycle block. E.g.,

.. code:: scala

    import com.example.thriftscala.ExampleThrift
    import com.twitter.conversions.DurationOps._
    import com.twitter.finatra.thrift.EmbeddedThriftServer
    import com.twitter.inject.server.FeatureTest
    import com.twitter.util.{Await, Duration}
    import scala.collection.immutable.ListMap

    class ExampleThriftServerFeatureTest extends FeatureTest {
      override val defaultAwaitTimeout: Duration = 2.seconds

      override val server = new EmbeddedThriftServer(
        twitterServer = new ExampleThriftServer,
        globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
        flags = Map(
          "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil")
      )

      lazy val client: ExampleThrift.ServicePerEndpoint =
        server.servicePerEndpoint[ExampleThrift.ServicePerEndpoint](clientId = "client123")

      ...

      override protected def afterAll(): Unit = {
        await(client.asClosable.close())
        super.afterAll()
      }

Note that the above example use a default `timeout` of `2.seconds` on awaiting the close of the test Thrift
client interface. You can and should adjust this value -- either up or down -- as appropriate for
your testing.

Combined `c.t.finatra.http.HttpServer` & `c.t.finatra.thrift.ThriftServer`
--------------------------------------------------------------------------

If you are extending both `c.t.finatra.http.HttpServer` **and** `c.t.finatra.thrift.ThriftServer`
then you can `FeatureTest` by constructing an `EmbeddedHttpServer with ThriftClient`, e.g.,

.. code:: scala

    import com.example.thriftscala.ExampleThrift
    import com.twitter.conversions.DurationOps._
    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.finatra.thrift.ThriftClient
    import com.twitter.inject.server.FeatureTest
    import scala.collection.immutable.ListMap

    class ExampleCombinedServerFeatureTest extends FeatureTest {
      override val server =
        new EmbeddedHttpServer(
          twitterServer = new ExampleCombinedServer,
          globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
          flags = Map(
            "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil")
        ) with ThriftClient

      lazy val client: ExampleThrift.MethodPerEndpoint =
        server.thriftClient[ExampleThrift.MethodPerEndpoint](clientId = "client123")

      "ExampleCombinedServer#perform feature") {
          server.httpGet(
            path = "/",
            andExpect = Status.Ok)
            ...
        }

       "ExampleCombinedServer#return data accordingly") {
          await(client.doExample("input")) should equal("output")
        }
      }
    }

Sharing a Server Fixture Between Many Feature Tests
---------------------------------------------------

There may be times in testing where you want to share an embedded server configuration among
different FeatureTests. That is, you want to be able to create and setup the embedded server in the
same way (perhaps with minor configuration changes) across many different test files.

One idea might be to define a "base" test trait which extends |c.t.inject.server.FeatureTest|_ that
your tests can extend.

Creating a "base" trait that defines shared state is a fine strategy. However, when doing so it
is generally considered a best practice to **not** share an instance of an embedded server.
That is, issues can arise when this "base" trait overrides and implements the |c.t.inject.server.FeatureTest|_
trait ``def server``.

Thus, we recommend to *always* implement the abstract ``def server`` in each *actual* `FeatureTest`
implementation.

This does not mean that you cannot share a configured embedded server fixture. To do so effectively
and efficiently, have the "base" trait define a utility method which allows a `FeatureTest`
implementation to obtain an instance of an embedded server fixture which it can then set as *its*
embedded server for testing.

For example, we could define a "base" testing trait:

.. code:: scala

    import com.twitter.inject.server.FeatureTest
    import com.twitter.finatra.http.EmbeddedHttpServer
    import scala.collection.immutable.ListMap

    trait BaseMyServiceFeatureTest extends FeatureTest {
      protected def foo: Foo
      protected def baz: Baz

      // Note, this merely provides a way for extensions of this trait to
      // get a commonly configurable EmbeddedHttpServer. Or it could define
      // an non-configurable version to ensure every test can use a similarly
      // configured server.
      protected def buildExampleServiceTestServer(
        name: String,
        flags: => Map[String, String] = Map()
      ): EmbeddedHttpServer =
        new EmbeddedHttpServer(new ExampleHttpServer {
          override val name = name
          override val overrideModules = Seq(???)
        },
        globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
        flags = flags
      ).bind[Foo].toInstance(foo)
       .bind[Baz].toInstance(baz)
    }

This "base" trait defines a method for obtaining a properly configured embedded server for test
implementations to use. Then in tests we could do:

.. code:: scala

    import com.twitter.util.mock.Mockito

    class MyServiceFirstFeatureTest extends BaseMyServiceFeatureTest with Mockito {
      override val foo: Foo = mock[Foo]
      override val baz: Baz = mock[Baz]

      // We override and implement the c.t.inject.server.FeatureTest#server as a val in our actual test file
      override val server = buildExampleServiceTestServer(
        "firstFeatureServer",
        Map("aaa.baz" -> "forty-two"))

      test("Feature 1 should do X") {
        ???
      }
    }

    ...

    class MyServiceOtherFeatureTest extends BaseMyServiceFeatureTest {
      override val foo: Foo = new DummyFoo()
      override val baz: Baz = new BazStub()

      override val server = buildExampleServiceTestServer(
        "secondFeatureServer"
        Map("aaa.baz" -> "thirty-five"))
      )

      test("Feature 2 should do Y") {
          ???
      }
    }

Reasons
~~~~~~~

Firstly, embedded servers close over specific configuration state (flags and other args) of the server
under test. Additionally, many servers are composed of JVM singletons (framework and potentially
user-defined) which expect to be the only instance present or running at any given time. That is,
there are no guarantees of thread-safety by default.

Thus, you can run into issues with inconsistent state of a shared embedded server fixture due to multiple
tests accessing it potentially in parallel. Semantics change depending on your build system and testing
framework, but it is generally a good practice to *not* share a single instance of an embedded server.

Secondly, when the server is defined as a `val` in a "base" trait from which many tests inherit,
**the same server can end up being started multiple times** -- even when you are attempting to run a
single test. Why? Some build systems optimize their test runs by first loading all test classes before
running a single test file or test case. When this occurs, all test classes will be instantiated and
thus any constructor `val` eagerly loaded. This could therefore start the embedded server `val`
in *each test* inheriting from the "base" trait and can generally lead to undesirable performance
when testing, thus the recommendation to always override the server member of `FeatureTest`
**in the actual test file**.

.. note::

   Finatra's testing utilities attempt to start servers lazily but any eager reference to the
   server's Injector would trigger the server to start in order to create and return the Injector.

Sharing Test Cases
~~~~~~~~~~~~~~~~~~

Note that you could also extend this in the situation where you want to run the same test cases over
**differently configured** instances of the same server. Your "base" trait would similarly provide
a method for obtaining a properly configured embedded server for test instances to use *as well as implement test cases*.

Each subclass implementation would then just cycle through different server configurations, setting
that configuration as it's own `server` under test.

For example, we could extend our defined "base" testing trait to include test cases:

.. code:: scala
    
    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.inject.server.FeatureTest
    import com.twitter.util.mock.Mockito
    import scala.collection.immutable.ListMap

    trait BaseMyServiceFeatureTest extends FeatureTest with Mockito {
      protected def foo: Foo = mock[Foo]
      protected def baz: Baz = mock[Baz]

      // provide a way to create a configured server under test
      protected def buildExampleServiceTestServer(
        name: String,
        flags: => Map[String, String] = Map()
      ): EmbeddedHttpServer =
        new EmbeddedHttpServer(new ExampleHttpServer {
          override val name = name
          override val overrideModules = Seq(???)
        },
        globalFlags = ListMap(com.some.globalFlag.disable -> "true"),
        flags = flags
      ).bind[Foo].toInstance(foo)
       .bind[Baz].toInstance(baz)

      // we want all subclasses to run these same tests

      test("Feature 1 should do it's thing") {
        // 'server' is defined as an abstract member in `FeatureTest` thus we can reference it here
        server.httpGet("/feature1", andExpect = Status.Ok)
      }

      test("Feature 2 should do it's thing") {
        server.httpGet("/feature2", andExpect = Status.Ok)
      }

      test("Feature 3 should do it's thing") {
        server.httpGet("/feature3", andExpect = Status.Ok)
      }
    }

Then in tests we would do:

.. code:: scala

    class MyServiceFirstFeatureTest extends BaseMyServiceFeatureTest {
      // We override and implement the c.t.inject.server.FeatureTest#server as a val in our actual test file
      override val server = buildExampleServiceTestServer(
        "firstFeatureServer",
        Map("aaa.baz" -> "forty-two"))
    }

    ...

    class MyServiceOtherFeatureTest extends BaseMyServiceFeatureTest {
      override val server = buildExampleServiceTestServer(
        "secondFeatureServer"
        Map("aaa.baz" -> "thirty-five"))
      )
    }

These subclasses would run the test cases from the superclass but each using their own server
configuration.

For more information on mocking, see the `Working with Mocks <./mocks.html>`_ documentation.

Injecting Members of a Test
---------------------------

.. warning::

    Do not inject members of a test class into the server, application or TestInjector object graph under test.

For an explanation of why, see the documentation `here <./bind_dsl.html#injecting-members-of-a-test>`__.

Examples:
---------

-  the
   `DoEverythingServerFeatureTest <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/test/DoEverythingServerFeatureTest.scala>`__
   for an HTTP server.
-  the
   `DoEverythingThriftServerFeatureTest <https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/tests/DoEverythingThriftServerFeatureTest.scala>`__
   for a Thrift server.
-  the
   `DoEverythingCombinedServerFeatureTest <https://github.com/twitter/finatra/blob/develop/inject-thrift-client-http-mapper/src/test/scala/com/twitter/finatra/multiserver/test/DoEverythingCombinedServerFeatureTest.scala>`__
   for "combined" HTTP and Thrift server.

More Information
----------------

- :doc:`index`
- :doc:`embedded`
- :doc:`integration_tests`
- :doc:`startup_tests`
- :doc:`mixins`
- :doc:`mocks`
- :doc:`override_modules`
- :doc:`bind_dsl`

.. |c.t.inject.server.FeatureTest| replace:: `c.t.inject.server.FeatureTest`
.. _c.t.inject.server.FeatureTest: https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala

.. |c.t.inject.server.FeatureTestMixin| replace:: `c.t.inject.server.FeatureTestMixin`
.. _c.t.inject.server.FeatureTestMixin: https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTestMixin.scala#L24

.. |EmbeddedTwitterServer| replace:: `EmbeddedTwitterServer`
.. _EmbeddedTwitterServer: https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala

.. |EmbeddedHttpServer| replace:: `EmbeddedHttpServer`
.. _EmbeddedHttpServer: https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/EmbeddedHttpServer.scala

.. |EmbeddedThriftServer| replace:: `EmbeddedThriftServer`
.. _EmbeddedThriftServer: https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/EmbeddedThriftServer.scala

.. |c.t.finatra.thrift.ThriftClient#thriftClient[T]| replace:: `c.t.finatra.thrift.ThriftClient#thriftClient[T]`
.. _c.t.finatra.thrift.ThriftClient#thriftClient[T]: https://github.com/twitter/finatra/blob/72664be4439da4425dfe63fa325f4c1ebbc5bf4b/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftClient.scala#L77

.. |c.t.finatra.thrift.ThriftClient#servicePerEndpoint[T]| replace:: `c.t.finatra.thrift.ThriftClient#servicePerEndpoint[T]`
.. _c.t.finatra.thrift.ThriftClient#servicePerEndpoint[T]: https://github.com/twitter/finatra/blob/72664be4439da4425dfe63fa325f4c1ebbc5bf4b/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftClient.scala#L103

.. |c.t.finatra.thrift.ThriftClient#methodPerEndpoint[T, U]| replace:: `c.t.finatra.thrift.ThriftClient#methodPerEndpoint[T, U]`
.. _c.t.finatra.thrift.ThriftClient#methodPerEndpoint[T, U]: https://github.com/twitter/finatra/blob/72664be4439da4425dfe63fa325f4c1ebbc5bf4b/thrift/src/test/scala/com/twitter/finatra/thrift/ThriftClient.scala#L134

.. |c.t.util.Await| replace:: `c.t.util.Await`
.. _c.t.util.Await: https://github.com/twitter/util/blob/54f314d1f4b37d302f685e99b1ac416e48532a04/util-core/src/main/scala/com/twitter/util/Awaitable.scala#L77

.. |c.t.util.Future| replace:: `c.t.util.Future`
.. _c.t.util.Future: https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala

.. |c.t.util.Await#ready| replace:: `c.t.util.Await#ready`
.. _c.t.util.Await#ready: https://github.com/twitter/util/blob/0d77572c76c7c54c0b10a1d25856af16148fe3c4/util-core/src/main/scala/com/twitter/util/Awaitable.scala#L140

.. |c.t.util.Await#result| replace:: `c.t.util.Await#result`
.. _c.t.util.Await#result: https://github.com/twitter/util/blob/54f314d1f4b37d302f685e99b1ac416e48532a04/util-core/src/main/scala/com/twitter/util/Awaitable.scala#L127

.. |c.t.server.TwitterServer| replace:: `c.t.server.TwitterServer`
.. _c.t.server.TwitterServer: https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala

.. |c.t.inject.server.TwitterServer| replace:: `c.t.inject.server.TwitterServer`
.. _c.t.inject.server.TwitterServer: https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/main/scala/com/twitter/inject/server/TwitterServer.scala
