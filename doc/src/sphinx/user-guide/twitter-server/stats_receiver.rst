.. _twitterserver_stats_receiver:

How to properly configure the server `StatsReceiver`
====================================================

The Finatra framework builds upon |TwitterServer|_ and for the most part functions as a direct
extension of |TwitterServer|_ features and functionality. There is, however, one important divergence
which is important to highlight when using the Finatra framework with `Guice <https://github.com/google/guice>`__
`dependency injection <https://en.wikipedia.org/wiki/Dependency_injection>`_.

`c.t.server.Stats#statsReceiver`
--------------------------------

.. warning::

    Do not set or access the `com.twitter.finagle.stats.StatsReceiver` via the `c.t.server.Stats#statsReceiver`
    trait when using the Finatra framework with `Guice <https://github.com/google/guice>`__
    `dependency injection <https://en.wikipedia.org/wiki/Dependency_injection>`_.

The |TwitterServer|_
`c.t.server.Stats <https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/Stats.scala>`__
trait provides access for implementations to override the defined `statsReceiver` which returns a
`com.twitter.finagle.stats.StatsReceiver`. This trait is mixed into the main `TwitterServer <https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala#L39>`__
trait and thus this definition is available for use in the server.

However, Finatra users using the framework with `Guice <https://github.com/google/guice>`__
`dependency injection <https://en.wikipedia.org/wiki/Dependency_injection>`_ **should not** configure
or access the server `com.twitter.finagle.stats.StatsReceiver` in this manner. Doing so will prevent
the framework from properly `replacing <../testing/embedded.html#inmemorystatsreceiver>`__ the `StatsReceiver` in testing.

Use a `StatsReceiverModule`
---------------------------

Instead, users should prefer to always interact with the `StatsReceiver` via the object graph.

The framework provides a `StatsReceiverModule <https://github.com/twitter/finatra/blob/develop/inject/inject-modules/src/main/scala/com/twitter/inject/modules/StatsReceiverModule.scala>`__
which by default binds the `StatsReceiver` type to the `LoadedStatsReceiver` instance and is added as
`a framework Module in the c.t.inject.server.Twitter <https://github.com/twitter/finatra/blob/bfc557786087e7acddc8dec9434cba440a4987b8/inject/inject-server/src/main/scala/com/twitter/inject/server/TwitterServer.scala#L86>`__.

To configure a specialized `StatsReceiver`, override the
`c.t.inject.server.Twitter#statsReceiverModule <https://github.com/twitter/finatra/blob/bfc557786087e7acddc8dec9434cba440a4987b8/inject/inject-server/src/main/scala/com/twitter/inject/server/TwitterServer.scala#L116>`__
with your customized implementation which will bind the `StatsReceiver` instance to the
object graph.

.. code:: scala

    import com.google.inject.Module
    import com.twitter.finagle.stats.{StatsReceiver, LoadedStatsReceiver}
    import com.twitter.inject.server.TwitterServer
    import com.twitter.inject.TwitterModule

    object MyCustomStatsReceiverModule extends TwitterModule {

      override def configure(): Unit = {
        bind[StatsReceiver].toInstance(LoadedStatsReceiver.scope("oursvr"))
      }
    }

    class MyServer extends TwitterServer {

      override val statsReceiverModule: Module = MyCustomStatsReceiverModule

      override protected def start(): Unit = {
        // It is important to remember to NOT BLOCK this method.
        val sr = injector.instance[StatsReceiver]
        ???
      }
    }


Access the `StatsReceiver` from the Object Graph
------------------------------------------------

Finatra users should also not refer directly to the `c.t.server.Stats#statsReceiver` as this again
will prevent proper testing of the server. Users should instead obtain the server `StatsReceiver`
instance from the object graph, e.g.:

Directly from the `Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__:

.. code:: scala

    import com.twitter.finagle.stats.StatsReceiver

    val sr = injector.instance[StatsReceiver]

Or as an injected constructor argument to a class:

.. code:: scala

    import com.twitter.finagle.stats.StatsReceiver
    import javax.inject.Inject

    class Foo @Inject() (statsReceiver: StatsReceiver) {
      ???
    }

Or in an `@Provides`-annotated method in a `TwitterModule`

.. code:: scala

    import com.google.inject.Provides
    import com.twitter.finagle.stats.StatsReceiver
    import com.twitter.inject.TwitterModule
    import javax.inject.Singleton

    object MyModule extends TwitterModule {
      @Provides
      @Singleton
      def providesBar(
        statsReceiver: StatsReceiver): Bar = {
        ???
      }
    }

.. |TwitterServer| replace:: `TwitterServer`
.. _TwitterServer: https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala

