.. _migrating_from_twitter_server:

Migrating from `c.t.server.TwitterServer`
=========================================

Introduction
------------

`c.t.server.TwitterServer <https://github.com/twitter/twitter-server/blob/e4acf5fe144d59ebae92e465c8b80e717f96a2b3/server/src/main/scala/com/twitter/server/TwitterServer.scala#L13>`__
is fairly straight-forward when it comes to what the framework provides in order to create a server.
You extend a trait and implement a main method.

A basic TwitterServer may look like this (from `TwitterServer: Getting Started <https://twitter.github.io/twitter-server/#getting-started>`__):

.. code:: scala

    import com.twitter.server.TwitterServer
    import com.twitter.finagle.Http
    import com.twitter.finagle.ListeningServer
    import com.twitter.finagle.Service
    import com.twitter.finagle.http.Request
    import com.twitter.finagle.http.Response
    import com.twitter.finagle.http.Status
    import com.twitter.util.Await
    import com.twitter.util.Future

    object BasicServer extends TwitterServer {
     val service = new Service[Request, Response] {
       def apply(request: Request) = {
         val response = Response(request.version, Status.Ok)
         response.contentString = "hello"
         Future.value(response)
       }
     }
     def main() {
       val server: ListeningServer = Http.serve(":8888", service)
       onExit {
         server.close()
       }
       Await.ready(server)
     }
    }

This will serve the defined "service" (a Finagle `Service <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Service.scala>`__
over the Finagle HTTP `Request <https://github.com/twitter/finagle/blob/develop/finagle-base-http/src/main/scala/com/twitter/finagle/http/Request.scala>`__
and `Response <https://github.com/twitter/finagle/blob/develop/finagle-base-http/src/main/scala/com/twitter/finagle/http/Response.scala>`__
types) using the HTTP protocol, accepting requests on port `8888` of the machine on which this code
is executed. When the "BasicServer" TwitterServer is gracefully shutdown, it will attempt to close
the created Finagle HTTP server.

More information on Finagle `Services & Filters <https://twitter.github.io/finagle/guide/ServicesAndFilters.html>`__.

`c.t.server.TwitterServer` Lifecycle
------------------------------------

`c.t.server.TwitterServer` has a nominal lifecycle but it is not strictly enforced (hence the common
problem of reading Flag values before they have been parsed). Since `c.t.server.TwitterServer`
extends `c.t.app.App <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala>`__
it inherits `c.t.app.App's` lifecycle defined by `c.t.app.App#main` (which delegates to
`c.t.app.App#nonExitingMain <https://github.com/twitter/util/blob/ce78eeb4eaed4408f1e0eff8de605a1927335395/util-app/src/main/scala/com/twitter/app/App.scala#L409>`__)
which at a high-level looks like this:

.. code::

    inits -> parse flags -> premains -> main -> postmains -> exits

This is detailed more in-depth in :ref:`lifecycle`.

The bulk of your `c.t.server.TwitterServer` logic is typically contained in your `TwitterServer#main`
implementation which is looked up and invoked `via reflection <https://github.com/twitter/util/blob/ce78eeb4eaed4408f1e0eff8de605a1927335395/util-app/src/main/scala/com/twitter/app/App.scala#L428>`__
by `c.t.app.App`. You can register multiple "inits", "premains", "postmains", or "exits". These are
`collected in sequence <https://github.com/twitter/util/blob/ce78eeb4eaed4408f1e0eff8de605a1927335395/util-app/src/main/scala/com/twitter/app/App.scala#L123>`__
by `c.t.app.App` and executed in order.

This can get very hard to reason about quickly if you have a complicated trait hierarchy.

Remember, that this lifecycle happens when the `c.t.app.App#main` is called. Thus a common mistake is
for users to attempt to read Flag values eagerly in class or object constructors or in an "init" block
all or which occur before Flag parsing and which will result in the "``flag read before parsed``"
warning or result in a fatal error if you have set `c.t.app.App#failFastOnFlagsNotParsed = true`.

c.t.server.Lifecycle.Warmup
~~~~~~~~~~~~~~~~~~~~~~~~~~~

In addition to inheriting `c.t.app.App`'s simple `lifecycle <../getting-started/lifecycle.html>`__,
`c.t.server.TwitterServer` also provides an additional lifecycle phase related to server warmup via
the `c.t.server.Lifecycle.Warmup <https://github.com/twitter/twitter-server/blob/e4acf5fe144d59ebae92e465c8b80e717f96a2b3/server/src/main/scala/com/twitter/server/Lifecycle.scala#L133>`__
trait.

This trait is meant for servers to signal when they have completed work done to warm up the server
before then accepting traffic. Bytes are passed as soon as the Finagle `ListeningServer <https://github.com/twitter/finagle/blob/1bd4c2dfcad97f6460e4b3fbd7beb48d9331bba4/finagle-core/src/main/scala/com/twitter/finagle/Server.scala#L13>`__
binds to a port. This can potentially cause garbage to be created as requests begin to flow into the
server and possibly trigger a **major garbage collection** negatively affecting requests being served
as the server reaches a steady state.

Thus, warmup is commonly used to exercise code paths of the server such that startup does not
negatively affect any initial traffic to the server. The `Lifecycle.Warmup` trait provides a utility
to trigger garbage collection which should ideally be run right before binding to the network port
to accept traffic.

Thus if we update our example we now have:

.. code:: scala

   import com.twitter.server.Lifecycle
   import com.twitter.server.TwitterServer
   import com.twitter.finagle.Http
   import com.twitter.finagle.ListeningServer
   import com.twitter.finagle.Service
   import com.twitter.finagle.http.Request
   import com.twitter.finagle.http.Response
   import com.twitter.finagle.http.Status
   import com.twitter.util.Await
   import com.twitter.util.Future

    object BasicServer extends TwitterServer with Lifecycle.Warmup {
     val service = new Service[Request, Response] {
       def apply(request: Request) = {
         val response = Response(request.version, Status.Ok)
         response.contentString = "hello"
         Future.value(response)
       }
     }
     def main() {
       prebindWarmup()
       warmupComplete()
       // post-warmup, bind to our port
       val server: ListeningServer = Http.serve(":8888", service)
       onExit {
         server.close()
       }
       Await.ready(server)
     }
    }

Here we make sure to call `prebindWarmup() <https://github.com/twitter/twitter-server/blob/e4acf5fe144d59ebae92e465c8b80e717f96a2b3/server/src/main/scala/com/twitter/server/Lifecycle.scala#L142>`__
and `warmupComplete() <https://github.com/twitter/twitter-server/blob/e4acf5fe144d59ebae92e465c8b80e717f96a2b3/server/src/main/scala/com/twitter/server/Lifecycle.scala#L149>`__
manually in the correct places before we call `Http.serve` which returns the bound `ListeningServer`.
This is to ensure we've done as much towards garbage collection before accepting traffic on our
external interface.

Still somewhat straight-forward...but you can see that it is starting to get easier to get wrong.
This is a *very* basic server and we're not using any Flags in this example.

`c.t.inject.server.TwitterServer` Lifecycle
-------------------------------------------

The "injectable" TwitterServer, `c.t.inject.server.TwitterServer <./index.html>`__ uses dependency
injection to create an object graph and thus attempts to ensure the `Injector <../getting-started/dependency_injection.html>`__ 
is correctly constructed before it is used by controlling the TwitterServer lifecycle. Therefore, 
the biggest shift from a "vanilla" `c.t.server.TwitterServer` to the "injectable" `c.t.inject.server.TwitterServer` 
is that the Finatra framework controls the `c.t.server.TwitterServer` lifecycle and provides callbacks 
for the user which will be run at the "appropriate" part in the server lifecycle.

At the same time, the framework also provides *more* (finer-grain) lifecycle callbacks within the
`main` TwitterServer phase including for warmup:

.. code::

    *warmup* ->
    beforePostWarmup (Lifecycle#prebindWarmup) ->
    *postWarmup* (bind and announce external interfaces) ->
    afterPostWarmup (Lifecycle#warmupComplete)

Users generally only need to be concerned about `warmup` and `postWarmup` (but can override the other callbacks if desired).

Much more detail on the enhanced lifecycle within an injectable TwitterServer available `here <../getting-started/lifecycle.html#c-t-inject-server-twitterserver-lifecycle>`__.

Because Finatra manages the lifecycle of the `TwitterServer`, you no longer place logic inside of a
`main` method but into the different lifecycle method callbacks accordingly. Taking the `c.t.app.App`
lifecycle from above we can expand to show Finatra's further breakdown of the `main` method into
the various additional lifecycle phases:

.. code::

    inits -> parse flags -> premains -> main -> postmains -> exits
                                         /\
    install modules -> modules.startup --> postInjectorStartup -> warmup -> beforePostWarmup -> postWarmup -> afterPostWarmup -> start -> onExit(modules.shutdown)

Next Steps
----------

.. important::

    When overriding any lifecycle methods and callbacks, it is important to **not perform any blocking
    operations** in your override as you will prevent the server from properly starting. If there is
    blocking work that must be done, it is strongly recommended that you perform this work in a
    `FuturePool <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/FuturePool.scala>`__.

    See the Finatra utility: |FuturePools|_ for creating named pools.

Migration entails moving (and possibly segmenting) the monolithic logic in your current `main()` method
into the different lifecycle callbacks available via the :ref:`lifecycle`. It is important to understand
the :ref:`lifecycle` to know what parts of your `main()` logic maps to which fine-grain Finatra lifecycle
callback.

See :ref:`injectable_twitter_server` for a walk-through of creating a new injectable TwitterServer
which explains what logic goes into which lifecycle callback.

From your original `main()` method, you now need to map to several different `lifecycle callback methods <./index.html#lifecycle-callbacks>`__.
Most commonly, this will mean splitting the logic between `setup()`, `warmup()`, `postWarmup()`, and `start()`. 
As per the documentation, :ref:`injectable_twitter_server` the primary logic of your `main()` will now be in 
`start()`.

The `setup()` method is called right after the `Injector <../getting-started/dependency_injection.html>`__ is created, 
`warmup()` is for ensuring that your code paths are exercised and that you can get in a garbage collection before 
serving any traffic. The `postWarmup()` is where we recommend starting and announcing any external interfaces 
(make sure that you `register any Awaitables <./index.html#awaiting-awaitables>`__). And finally `start()` is for 
any remaining logic that must run. 

More details `here <./index.html#overriding-server-lifecycle-functions>`__.

Testing
-------

Finatra provides rich support for `testing a c.t.server.TwitterServer <../testing/feature_tests.html#c-t-server-twitterserver>`__ and 
even more utility is available for `testing an "injectable" c.t.inject.server.TwitterServer <../testing/feature_tests.html#c-t-inject-server-twitterserver>`__.

For details see the `Testing with Finatra <../testing/index.html>`__ section.


.. |FuturePools| replace:: ``c.t.finatra.utils.FuturePools``
.. _FuturePools: https://github.com/twitter/finatra/blob/develop/utils/src/main/scala/com/twitter/finatra/utils/FuturePools.scala
