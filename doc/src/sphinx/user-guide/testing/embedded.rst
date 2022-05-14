.. _embedded:

Embedded Servers and Apps
=========================

.. important::

  Please see the section on including test-jar dependencies in your project: `Test Dependencies <../..#test-dependencies>`_.

Finatra provides a way to run an embedded version of your service or app running locally on ephemeral
ports. This allows you to run *actual* requests against an *actual* version of your server when testing.

The embedded utilities are also useful for testing and debugging your code when prototyping. If your
service or API makes calls to other services, instead of mocking out or overriding those dependencies
with dummy implementations you can always write a test using an Embedded version of your server which
talks to *real* downstream services (of course you'd never want to commit a test like this to your
source repository, especially if you run any type of `continuous integration <https://en.wikipedia.org/wiki/Continuous_integration>`__ system).
You'll be able to run this test normally through the test runner of an IDE which would allow you to
easily set breakpoints and step-through code for debugging. As opposed to needing to build and run
your service locally and attach a remote debugger.

See:

-  `c.t.inject.app.EmbeddedApp <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/test/scala/com/twitter/inject/app/EmbeddedApp.scala>`__
-  `c.t.inject.server.EmbeddedTwitterServer <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala>`__
-  `c.t.finatra.http.EmbeddedHttpServer <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/EmbeddedHttpServer.scala>`__
-  `c.t.finatra.thrift.EmbeddedThriftServer <https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/EmbeddedThriftServer.scala>`__


.. image:: ../../_static/embedded.png

You'll notice that this hierarchy generally follows the server trait hierarchy as |c.t.finatra.http.HttpServer|_
and |c.t.finatra.thrift.ThriftServer|_ extend from |c.t.server.TwitterServer|_ which extends from
|c.t.app.App|_.

Testing With `Global Flags`
---------------------------

The embedded servers and the embedded app allow for passing `Twitter Util <https://github.com/twitter/util>`__ 
`Flags <https://github.com/twitter/util/blob/1dd3e6228162c78498338b1c3aa11afe2f8cee22/util-app/src/main/scala/com/twitter/app/Flag.scala>`__
to the server under test via the `flags <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L130>`__
constructor argument (a map of flag name to flag value) which is meant to mimic setting flag values
via the command line.

However it is **not recommended** that users set any |GlobalFlag|_ value in this manner when testing. In 
normal usage, the value of a |GlobalFlag|_ is **only read once during the initialization of the JVM process**.
Which means that setting the value of a |GlobalFlag|_ as a normal flag on a server will set it for the JVM 
process started by the test harness and the value will not be able to be reset during the lifetime of the process. 
This also means that the value of the |GlobalFlag|_ would be wholly dependent on which test server is started 
first in the test suite, increasing the likelihood of non-deterministic effects.

A |GlobalFlag|_ can be scoped for the execution of an embedded server by passing in a value via the
`globalFlags <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L142>`__
arg of an embedded server. For example,

.. code:: scala

    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.finagle.http.Status
    import com.twitter.inject.server.FeatureTest
    import scala.collection.immutable.ListMap

    class ExampleServerFeatureTest extends FeatureTest {
      override val server = new EmbeddedHttpServer(
        twitterServer = new ExampleServer,
        globalFlags = ListMap(com.foo.bar.someGlobalFlag -> "a value")
      )

      test("ExampleServer#perform feature") {
        // any read of the `com.foo.bar.someGlobalFlag` value within the server will be "a value"
        server.httpGet(
          path = "/",
          andExpect = Status.Ok)

        ???
      }
    }

.. important::

    Global Flags are applied in insertion order with the first entry being applied "closest" to
    the startup of the underlying `TwitterServer`. Thus to maintain insertion order of passed
    flags, it is recommended that users use a `scala.collection.immutable.ListMap` or equivalent.

If you wish to test with toggled values of a |GlobalFlag|_ for an embedded app or server, you can
still use |FlagLet|_ or |FlagLetClear|_ in tests. Note that if an embedded server takes
`globalFlags <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L142>`__
arguments, they will take precedents over an outer |FlagLet|_. It is recommended to test using
|FlagLet|_ or |FlagLetClear|_ on a |GlobalFlag|_ in places where the global flag value is read once and not
treated as a runtime variable, otherwise behavior can be unexpected across thread boundaries.
For example,

.. code:: scala

    import com.twitter.finatra.http.EmbeddedHttpServer
    import com.twitter.finagle.http.Status
    import com.twitter.inject.server.FeatureTest
    import scala.collection.immutable.ListMap

    class ExampleServerFeatureTest extends FeatureTest {
      override val server = new EmbeddedHttpServer(
        twitterServer = new ExampleServer,
        globalFlags = ListMap(com.foo.bar.someGlobalFlag -> "a value")
      )

      test("ExampleServer#perform feature") {

        com.foo.bar.someGlobalFlag.let("b value") {
          // any read of the `com.foo.bar.someGlobalFlag` value in this closure will be "b value"

          com.foo.bar.someGlobalFlag() should equal("b value")

          // except execution within the server will see "a value" due to `globalFlags` scope
          server.httpGet(
            path = "/",
            andExpect = Status.Ok)

          ???
        }
      }
    }

See the `scaladoc <https://twitter.github.io/util/docs/com/twitter/app/Flag.html>`_ for `c.t.app.Flag`
for more information on using |FlagLet|_ or |FlagLetClear|_.

InMemoryStatsReceiver
---------------------

The |EmbeddedTwitterServer|_ (and thus its subclasses: |EmbeddedHttpServer|_ and |EmbeddedThriftServer|_)
binds an instance of the `com.twitter.finagle.stats.InMemoryStatsReceiver <https://github.com/twitter/util/blob/develop/util-stats/src/main/scala/com/twitter/finagle/stats/InMemoryStatsReceiver.scala>`__
to the underlying server's object graph (if the underlying server supports injection). This will
override any other bound implementation of a `c.t.finagle.stats.StatsReceiver <https://github.com/twitter/util/blob/develop/util-stats/src/main/scala/com/twitter/finagle/stats/StatsReceiver.scala>`__
in the server's object graph.

The |EmbeddedTwitterServer|_ exposes the bound `StatsReceiver <https://github.com/twitter/util/blob/develop/util-stats/src/main/scala/com/twitter/finagle/stats/StatsReceiver.scala>`__
along with helper methods for asserting `counter <https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L323>`__,
`stat <https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L335>`__,
and `gauge <https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala#L343>`__
values, such that you can expect behavior against the underlying server's recorded stats in tests.

`Feature Tests <#feature_tests>`__ also `print all recorded stats <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTestMixin.scala#L42>`__
to stdout after each test, if the `printStats` property is set to `true` (it is disabled by default to minimize test execution times).

See: `c.t.finatra.multiserver.test.MultiServerFeatureTest <https://github.com/twitter/finatra/blob/develop/inject-thrift-client-http-mapper/src/test/scala/com/twitter/finatra/multiserver/test/MultiServerFeatureTest.scala>`__
for an example usage.

InMemoryTracer
---------------------

The |EmbeddedTwitterServer|_ (and thus its subclasses: |EmbeddedHttpServer|_ and |EmbeddedThriftServer|_)
binds an instance of the `com.twitter.finagle.stats.InMemoryTracer <https://github.com/twitter/util/blob/develop/util-stats/src/main/scala/com/twitter/finagle/stats/InMemoryTracer.scala>`__
to the underlying server's object graph (if the underlying server supports injection). This will
override any other bound implementation of a `c.t.finagle.tracing.Tracer <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/tracing/Tracer.scala>`__
in the server's object graph.

The |EmbeddedTwitterServer|_ exposes the bound `Tracer <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/tracing/Tracer.scala>`__
along with helper methods for verifying the presence of Trace `Record <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/tracing/Record.scala>`__
values, such that you can inspect behavior against the underlying server's recorded trace annotations in tests.

`Feature Tests <#feature_tests>`__ also `print all recorded traces <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTestMixin.scala#L54>`__
to stdout after each test, if the `printTraces` property is set to `true` (it is disabled by default to minimize test execution times).

See: `c.t.finatra.multiserver.test.MultiServerFeatureTest <https://github.com/twitter/finatra/blob/develop/inject-thrift-client-http-mapper/src/test/scala/com/twitter/finatra/multiserver/test/MultiServerFeatureTest.scala>`__
for an example usage.

Injecting Members of a Test
---------------------------

.. warning::

    Do not inject members of a test class into the server or application under test.

For an explanation of why, see the documentation `here <./bind_dsl.html#injecting-members-of-a-test>`__.

More Information
----------------

- :doc:`index`
- :doc:`feature_tests`
- :doc:`integration_tests`
- :doc:`startup_tests`
- :doc:`mixins`
- :doc:`mocks`
- :doc:`override_modules`
- :doc:`bind_dsl`

.. |c.t.finatra.http.HttpServer| replace:: `c.t.finatra.http.HttpServer`
.. _c.t.finatra.http.HttpServer: https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/HttpServer.scala

.. |c.t.finatra.thrift.ThriftServer| replace:: `c.t.finatra.thrift.ThriftServer`
.. _c.t.finatra.thrift.ThriftServer: https://github.com/twitter/twitter-server/blob/develop/src/main/scala/com/twitter/server/TwitterServer.scala

.. |c.t.server.TwitterServer| replace:: `c.t.server.TwitterServer`
.. _c.t.server.TwitterServer: https://github.com/twitter/twitter-server/blob/develop/src/main/scala/com/twitter/server/TwitterServer.scala

.. |c.t.app.App| replace:: `c.t.app.App`
.. _c.t.app.App: https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala

.. |c.t.inject.app.App| replace:: `c.t.inject.app.App`
.. _c.t.inject.app.App: https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala

.. |EmbeddedTwitterServer| replace:: `EmbeddedTwitterServer`
.. _EmbeddedTwitterServer: https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala

.. |EmbeddedHttpServer| replace:: `EmbeddedHttpServer`
.. _EmbeddedHttpServer: https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/EmbeddedHttpServer.scala

.. |EmbeddedThriftServer| replace:: `EmbeddedThriftServer`
.. _EmbeddedThriftServer: https://github.com/twitter/finatra/blob/develop/thrift/src/test/scala/com/twitter/finatra/thrift/EmbeddedThriftServer.scala

.. |GlobalFlag| replace:: `GlobalFlag`
.. _GlobalFlag: https://github.com/twitter/util/blob/f2a05474ec41f34146d710bdc2a789efd6da9d21/util-app/src/main/scala/com/twitter/app/GlobalFlag.scala

.. |FlagLet| replace:: `Flag.let`
.. _FlagLet: https://twitter.github.io/util/docs/com/twitter/app/Flag.html#let[R](t:T)(f:=%3ER):R

.. |FlagLetClear| replace:: `Flag.letClear`
.. _FlagLetClear: https://twitter.github.io/util/docs/com/twitter/app/Flag.html#letClear[R](f:=%3ER):R
