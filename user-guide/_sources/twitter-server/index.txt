.. _twitter_server:

TwitterServer Admin HTTP Server
===============================

All Finatra servers are `TwitterServer <https://github.com/twitter/twitter-server>`__-based servers and thus it helps to understand some basics of a `TwitterServer <https://github.com/twitter/twitter-server>`__.

HTTP Admin Interface
--------------------

`TwitterServer <https://github.com/twitter/twitter-server>`__-based servers have the option to start an `HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#admin-http-interface>`__ bound to a port configurable via the `-admin.port` flag. If you want to serve an external interface this will be bound to a *separate* port configurable via either the `-http.port`, `-https.port` or `-thrift.port` flags, depending.

For more information on using and setting command-line flags see `Flags <../getting-started/flags.html#passing-flag-values-as-command-line-arguments>`__.

Disabling the TwitterServer AdminHttpServer
-------------------------------------------

Some deployment environments such as `Heroku <https://www.heroku.com/>`__, `AppFog <https://www.appfog.com/>`__, and `OpenShift <https://www.openshift.com>`__ only allow a single port to be used when deploying an application. 

In these cases, you can programmatically disable the TwitterServer `HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#http-admin-interface>`__:

.. code:: scala

    class ExampleServer extends HttpServer {
      override val disableAdminHttpServer = true
      ...
    }

Note:
^^^^^

Since the `-admin.port` flag is currently still required to have a value by `TwitterServer <https://github.com/twitter/twitter-server>`__ you should set the `-admin.port` and your external interface port e.g., `-http.port` flags to the **same value** when disabling the TwitterServer AdminHttpServer.

For a working example of disabling the TwitterServer `HTTP Admin Interface <https://twitter.github.io/twitter-server/Features.html#http-admin-interface>`__, see the `Heroku hello-world example <https://github.com/twitter/finatra/tree/master/examples/hello-world-heroku>`__.