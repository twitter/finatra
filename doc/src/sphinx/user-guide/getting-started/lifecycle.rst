.. _lifecycle:

Application and Server Lifecycle
================================

Finatra establishes an ordered lifecycle when creating a `c.t.app.App <https://twitter.github.io/util/docs/com/twitter/app/App.html>`__ or
a `c.t.server.TwitterServer <https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala>`__
and provides methods which can be implemented for running or starting core logic.

This is done for several reasons:

- To ensure that `flag <./flags.html>`__ parsing and `module <./modules.html>`__ installation to
  build the object graph is done in the correct order such that the injector is properly configured
  before a user attempts attempts to access flags.

- Ensure that object promotion and garbage collection is properly handled *before* accepting traffic
  to a server.

- Expose any external interface *before* reporting a server is "healthy". Otherwise a server may
  report it is healthy before binding to a port — which may fail. Depending on how monitoring is
  configured (typically done by monitoring the `HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#admin-http-interface>`__
  `/health <https://twitter.github.io/twitter-server/Features.html#lifecycle-management>`__ endpoint
  on some frequency) it could be some interval before the server is recognized as unhealthy when in
  fact it did not start properly as it could not bind to a port.

Thus you do not have access to the `App <https://github.com/twitter/util/blob/9fa550a269d2287b24e94921a352ba954f9f4bfb/util-app/src/main/scala/com/twitter/app/App.scala#L24>`__
or `TwitterServer <https://github.com/twitter/twitter-server/blob/5fea9c2a6220ab9bbdb449c99c946e2aef322e7d/server/src/main/scala/com/twitter/server/TwitterServer.scala#L93>`__
`main` method. Instead, any logic should be contained in overriding an ``@Lifecycle``-annotated
method or in the application or server callbacks.

.. caution::

   If you override an ``@Lifecycle``-annotated method you **MUST** first call
   `super.lifecycleMethod()` in your override to ensure that framework lifecycle events happen
   accordingly.

See the `Creating an injectable App <../app/index.html>`__ and
`Creating an injectable TwitterServer <../twitter-server/index.html>`__ sections for more information.

Startup
-------

At a high-level, the start-up lifecycle of a Finatra server looks like:

.. image:: ../../_static/FinatraLifecycle.png

Shutdown
--------

Upon *graceful* shutdown of an application or a server, all registered `onExit`, `closeOnExit`, and
`closeOnExitLast` blocks are executed. See
`c.t.app.App#exits <https://github.com/twitter/util/blob/9fa550a269d2287b24e94921a352ba954f9f4bfb/util-app/src/main/scala/com/twitter/app/App.scala#L72>`__
and `c.t.app.App#lastExits <https://github.com/twitter/util/blob/bf47b55ff45a31bbd541f66257f2244df5c35f5b/util-app/src/main/scala/com/twitter/app/App.scala#L86>`_.

For a server, this includes closing the `TwitterServer <https://github.com/twitter/twitter-server>`__
`HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#admin-http-interface>`__
and shutting down and closing all installed modules. For extensions of the
`HttpServer <../http/server.html>`__ or `ThriftServer <../thrift/server.html>`__ traits this also
includes closing any external interfaces.

.. admonition:: Important

   Note that the order of execution for all registered `onExit` and `closeOnExit` blocks is not
   guaranteed as they are executed on graceful shutdown roughly in parallel. Thus it is up to
   implementors to enforce any desired ordering.

For example, you have code which is reading from a queue (via a "subscriber"), transforming the
data, and then publishing (via a "publisher") to another queue. When the main application is exiting
you most likely want to close the "subscriber" first to ensure that you transform and publish all
available data before closing the "publisher".

Assuming, that both objects are a |c.t.util.Closable|_ type, a simple way to close them would be:

.. code:: scala

     closeOnExit(subscriber)
     closeOnExit(publisher)

However, the "subscriber" and the "publisher" would close roughly in parallel
which could lead to data inconsistencies in your server if the "subscriber" is still reading before
the "publisher" has closed.

Ordering `onExit` and `closeOnExit` functions?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Assuming, that the `#close()` method of both returns `Future[Unit]`, e.g. like a |c.t.util.Closable|_,
a way of doing this could be:

.. code:: scala

    onExit {
      Await.result(subscriber.close(defaultCloseGracePeriod))
      Await.result(publisher.close(defaultCloseGracePeriod))
    }

where the `defaultCloseGracePeriod` is the `c.t.app.App#defaultCloseGracePeriod <https://github.com/twitter/util/blob/bf47b55ff45a31bbd541f66257f2244df5c35f5b/util-app/src/main/scala/com/twitter/app/App.scala#L110>`__
function.

In the above example we simply await on the `#close()` of the "subscriber" first and then the
`#close()` of the "publisher" thus ensuring that the "subscriber" will close before the "publisher".

However, we are not providing a timeout to the `Await.result`, which we should ideally do as
well since we do not want to accidentally block our server shutdown if the `defaultCloseGracePeriod`
is set to something high or infinite (e.g., `Time.Top <https://github.com/twitter/util/blob/bf47b55ff45a31bbd541f66257f2244df5c35f5b/util-core/src/main/scala/com/twitter/util/Time.scala#L302>`__).

But if we don't know the configured value of the  `defaultCloseGracePeriod` this makes things
complicated. We could just hardcode a value for the Await, or not use the `defaultCloseGracePeriod`:

.. code:: scala

    onExit {
      Await.result(subscriber.close(defaultCloseGracePeriod), 5.seconds)
      Await.result(publisher.close(defaultCloseGracePeriod), 5.seconds)
    }

    ...

    onExit {
      Await.result(subscriber.close(4.seconds), 5.seconds)
      Await.result(publisher.close(4.seconds), 5.seconds)
    }

However, this is obviously not ideal and there is an easier way. You can enforce the ordering of closing Closables
by using `closeOnExitLast`.

A |c.t.util.Closable|_ passed to `closeOnExitLast` will be closed *after* all `onExit` and
`closeOnExit` functions are executed. E.g.,

.. code:: scala

     closeOnExit(subscriber)
     closeOnExitLast(publisher)

In this code the "publisher" is guaranteed be closed **after** the "subscriber".

.. note:: All the exit functions: `onExit`, `closeOnExit`, and `closeOnExitLast` use the
    `defaultCloseGracePeriod` as their close "deadline" and will raise a `TimeoutException` if
    all the `exits` (collected `onExit`, `closeOnExit` functions) do not close within the deadline.
    And if the `lastExits` (collected `closeOnExitLast` functions) do not close within the deadline.

If you have multiple |c.t.util.Closable|_ objects you want to close in parallel and one you want to
close after all the others, you could do:

.. code:: scala

     closeOnExit(subscriberA)
     closeOnExit(subscriberB)
     closeOnExit(subscriberC)
     closeOnExitLast(publisher)

The "publisher" is guaranteed be closed **after** the closing of "subscriberA", "subscriberB", and
"subscriberC".

What to do if you don't have a |c.t.util.Closable|_?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can simply use the `onExit` block to perform any shutdown logic, or you can wrap a function in
a |c.t.util.Closable|_ to be passed to `closeOnExit` or `closeOnExitLast`.

For example:

.. code:: scala

    onExit {
       DatabaseConnection.drain()
       Await.result(someFutureOperation, 2.seconds)
    }

    closeOnExit {
      Closable.make { deadline =>
       prepWork.start()
       anotherFutureOperation
      }
    }

    closeOnExitLast {
      Closable.make { deadline =>
        queue.blockingStop(deadline)
        Future.Unit
      }
    }

You can also wrap multiple functions in a Closable:

.. code:: scala

    closeOnExit {
       Closable.make { deadline =>
         database.drain()
         fileCleanUp.do()
         pushData(deadline)
         Future.Unit
       }
    }

Again the code in `onExit` and `closeOnExit` will be run in parallel and guaranteed to close
before the functions in `closeOnExitLast`.

.. note:: Multiple `closeOnExitLast` Closables will be closed in parallel with each other but
    **after** all `onExit` and `closeOnExit` functions have closed.

Modules
-------

Modules provide hooks into the Lifecycle as well that allow instances being provided to the object
graph to be plugged into the overall application or server lifecycle. See the
`Module Lifecycle <../getting-started/modules.html#module-lifecycle>`__ section for more information.

More Information
----------------

As noted in the diagram in the `Startup <#startup>`__ section the lifecycle or an application can be
non-trivial -- especially in the case of a `TwitterServer <https://github.com/twitter/twitter-server>`__.

For more information on how to create an injectable `c.t.app.App <https://twitter.github.io/util/docs/com/twitter/app/App.html>`__
or a `c.t.server.TwitterServer <https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala>`__
see the `Creating an injectable App <../app/index.html>`__ and
`Creating an injectable TwitterServer <../twitter-server/index.html>`__ sections.

.. |c.t.util.Closable| replace:: `c.t.util.Closable`
.. _c.t.util.Closable: https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Closable.scala
