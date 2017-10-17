.. _lifecycle:

Application and Server Lifecycle
================================

Finatra establishes an ordered lifecycle when creating a `c.t.app.App <https://twitter.github.io/util/docs/com/twitter/app/App.html>`__ or
a `c.t.server.TwitterServer <https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala>`__
and provides methods which can be implemented for running or starting core logic.

This is done for several reasons:

- To ensure that flag parsing and module installation to build the object graph is done in the correct
  order such that the injector is properly configured for use before a user attempts attempts to access flags.

- Ensure that object promotion and garbage collection is properly handled *before* accepting traffic to a server.

- Expose any external interface *before* reporting a server is "healthy". Otherwise a server may
  report it is healthy before binding to a port — which may fail. Depending on how monitoring is
  configured (typically done by monitoring the `HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#admin-http-interface>`__
  `/health <https://twitter.github.io/twitter-server/Features.html#lifecycle-management>`__ endpoint
  on some frequency) it could be some interval before the server is recognized as unhealthy when in
  fact it did not start properly as it could not bind to a port.

Thus you do not have access to the `app <https://github.com/twitter/util/blob/9fa550a269d2287b24e94921a352ba954f9f4bfb/util-app/src/main/scala/com/twitter/app/App.scala#L24>`__
or `server <https://twitter.github.io/twitter-server/#getting-started>`__ `main` method. Instead, any
logic should be contained in overriding an ``@Lifecycle``-annotated method or in the app or server
callbacks |c.t.inject.app.App#run|_ or |c.t.inject.server.TwitterServer#start|_ methods, respectively.

.. caution::

   If you override an ``@Lifecycle``-annotated method you **MUST** first call `super()` in the method to ensure that framework lifecycle events happen accordingly.

   With all lifecycle methods, it is important to **not performing any blocking operations** as you will prevent the server from starting.

Startup
-------

At a high-level, the start-up lifecycle of a Finatra server looks like:

.. image:: ../../_static/FinatraLifecycle.png

Shutdown
--------

Upon *graceful* shutdown, all registered `onExit {...}` blocks are executed. See `c.t.app.App#exits <https://github.com/twitter/util/blob/9fa550a269d2287b24e94921a352ba954f9f4bfb/util-app/src/main/scala/com/twitter/app/App.scala#L72>`__).

This includes closing the `TwitterServer <https://github.com/twitter/twitter-server>`__ `HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#admin-http-interface>`__, firing the `TwitterModuleLifecycle#singletonShutdown <https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleLifecycle.scala#L29>`__
on all installed modules, and for extensions of the `HttpServer <../http/server.html>`__ or `ThriftServer <../thrift/server.html>`__ traits closing any external interfaces.

.. admonition:: Important

   Note that the order of execution for all registered `onExit {...}` blocks is not guaranteed. Thus it is, up to implementors to enforce any desired ordering.

   For example, you have code which is reading from a queue (via a "listener"), transforming the data, and then publishing (via a "publisher") to another queue.
   When main application is exiting you most likely want to close the "listener" first to ensure that you transform and publish all available data before closing the "publisher".


   Assuming, that the `close()` method of both returns `Future[Unit]`, e.g. like `c.t.util.Closable <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Closable.scala>`__,
   a way of doing this could be:

   .. code:: Scala

     onExit {
        Await.result(listener.close())
        Await.result(publisher.close())
     }

   In the example code above, we simply await on the close of the "listener" first and then the "publisher" thus ensuring that the "listener"
   will close before the "publisher".

   This is instead of registering separate `onExit {...}` blocks for each:

   .. code:: Scala

        onExit {
           Await.result(listener.close())
        }

        onExit {
           Await.result(publisher.close())
        }

   Which in this example, would possibly close the "publisher" before the "listener" meaning that the server would still be reading data but unable to publish it.

Modules
-------

Modules provide hooks into the Lifecycle as well that allow instances being provided to the object graph to be plugged into the overall application or server lifecycle. See the `Module Lifecycle <../getting-started/modules.html#module-lifecycle>`__ section for more information.

More Information
----------------

As noted in the diagram in the `Startup <#startup>`__ section there can be a non-trivial lifecycle especially in the case of a `TwitterServer <https://github.com/twitter/twitter-server>`__.
For more information on how to create an injectable `c.t.app.App <https://twitter.github.io/util/docs/com/twitter/app/App.html>`__ or a `c.t.server.TwitterServer <https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala>`__
see the `Creating an injectable App <../app/index.html>`__ and `Creating an injectable TwitterServer <../twitter-server/index.html>`__ sections.

.. |c.t.inject.app.App#run| replace:: ``c.t.inject.app.App#run``
.. _c.t.inject.app.App#run: ../app/index.html#app-run

.. |c.t.inject.server.TwitterServer#start| replace:: ``c.t.inject.server.TwitterServer#start``
.. _c.t.inject.server.TwitterServer#start: ../twitter-server/index.html#twitterserver-start
