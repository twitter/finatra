.. _twitter_server:

TwitterServer Basics
====================

All Finatra servers are `TwitterServer <https://github.com/twitter/twitter-server>`__-based servers
and thus it helps to understand some basics of a `TwitterServer <https://github.com/twitter/twitter-server>`__.

HTTP Admin Interface
--------------------

`TwitterServer <https://github.com/twitter/twitter-server>`__-based servers have the option to start
an `HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#admin-http-interface>`__
bound to a port configurable via the `-admin.port` flag. If you want to serve an external interface
this will be bound to a *separate* port configurable via either the `-http.port`, `-https.port` or
`-thrift.port` flags, depending.

For more information on using and setting command-line flags see `Flags <../getting-started/flags.html#passing-flag-values-as-command-line-arguments>`__.

Disabling the TwitterServer `AdminHttpServer <https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/AdminHttpServer.scala>`__
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------

Some deployment environments such as `Heroku <https://www.heroku.com/>`__, `AppFog <https://www.appfog.com/>`__,
and `OpenShift <https://www.openshift.com>`__ only allow a single port to be used when deploying an
application.

In these cases, you can programmatically disable the TwitterServer `HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#http-admin-interface>`__:

.. code:: scala

    class ExampleServer extends HttpServer {
      override val disableAdminHttpServer: Boolean = true
      ...
    }

.. note::

  Since the `-admin.port` flag is currently still required to have a value by `TwitterServer <https://github.com/twitter/twitter-server>`__
  you should set the `-admin.port` flag and your external interface port e.g., `-http.port` flag to
  the **same value** when disabling the TwitterServer `AdminHttpServer <https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/AdminHttpServer.scala>`__.

  For a working example of disabling the TwitterServer `HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#http-admin-interface>`__, see the `Heroku hello-world example <https://github.com/twitter/finatra/tree/master/examples/hello-world-heroku>`__.

Lifecycle Management
--------------------

While `TwitterServer <https://github.com/twitter/twitter-server>`__ provides a `Lifecycle <https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/Lifecycle.scala>`__
trait which exposes utilities to manage the lifecycle of your server it does not enforce how they
are used.

Finatra orders the `TwitterServer <https://github.com/twitter/twitter-server>`__ lifecycle events,
including `promoting OldGen <https://github.com/twitter/twitter-server/blob/15e35a3a3070c50168ff55fd83a2dff28b09795c/server/src/main/scala/com/twitter/server/Lifecycle.scala#L48>`__
and `performing initial garbage collection <https://github.com/twitter/twitter-server/blob/15e35a3a3070c50168ff55fd83a2dff28b09795c/server/src/main/scala/com/twitter/server/Lifecycle.scala#L67>`__
before your server accepts traffic, making sure you bind any external interface before reporting your
server is healthy, among other things.

How Finatra orders the lifecycle and uses the `TwitterServer <https://github.com/twitter/twitter-server>`__
`Lifecycle <https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/Lifecycle.scala>`__
trait functions is detailed in the `Application and Server Lifecycle <../getting-started/lifecycle.html>`__
section.
