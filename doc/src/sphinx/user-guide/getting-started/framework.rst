.. _framework:

Understanding the Codebase
==========================

Finatra is divided into three main parts: ``inject``, ``http``, and ``thrift``. The `inject` part of
the framework contains all of the base functionality for integration of the `Guice <https://github.com/google/guice>`__
dependency injection framework with the `TwitterUtil <https://twitter.github.io/util/>`__
|c.t.app.App|_ and the |TwitterServer|_ |c.t.server.TwitterServer|_.

Modules in the `inject` portion of the Finatra framework constitute the core of the framework, are
in the ``com.twitter.inject`` package namespace, and only depend on each other or outside dependencies
(except in testing). The `inject` layer can be thought of as dealing with concerns that are common
to the framework or that span both http and thrift services.

Everything else in the Finatra codebase is in the ``com.twitter.finatra`` package namespace and has
dependencies on the `inject` libraries as well as other libraries.

Inject
------

Modules in `inject` include:

- `inject-core` - core library which defines the `TwitterModule <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModule.scala>`__,
  `Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__,
  and the framework `TwitterModuleFlags <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleFlags.scala>`__
  for defining injectable `TwitterUtil <https://github.com/twitter/util>`__ `Flags <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala>`__.
- `inject-modules` - core library which defines framework modules.
- `inject-app` - core integration with `c.t.app.App <https://twitter.github.io/util/docs/com/twitter/app/App.html>`__
- `inject-server` - core integration with `c.t.server.TwitterServer`.
- `inject-slf4j` - core library integration with the `SLF4J <http://www.slf4j.org/manual.html>`__ API.
  Also provides support for bridging Logback's `Mapped Diagnostic Context <http://logback.qos.ch/manual/mdc.html>`__
  with Finagle. See `Mapped Diagnostic Context Filter <../logging/logback.html#mapped-diagnostic-context-filter>`__
  Logback documentation for more information.
- `inject-request-scope` - core library for integrating `Guice <https://github.com/google/guice>`__'s
  `Request Scope` with `TwitterUtil <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala>`__
  `c.t.util.Future`. See the `Request Scope <../http/filters.html#request-scope>`__ section for more information.
- `inject-thrift` - core library for thrift utilities useful for both clients and server.
- `inject-thrift-client` - core library for supporting creation of a Thrift `Finagle Client <https://twitter.github.io/finagle/guide/Clients.html>`__.
- `inject-utils` - core library which provides a collection of useful utilities, including implicit
  conversions, exceptions and futures handling and retries.

Creating an injectable App or TwitterServer
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

See: `Building an injectable c.t.app.App <../app/index.html>`__ or `Building an injectable c.t.server.TwitterServer <.../twitter-server/html>`__.

.. important::

  It is important to remember that the Finatra framework establishes an order of the lifecycle of an
  application or server. See the `Application and Server Lifecycle <lifecycle.html>`__ section for
  more information.

Jackson
-------

Finatra's integration with `jackson-module-scala <https://github.com/FasterXML/jackson-module-scala>`__
for JSON support. See the `Jackson Integration <../json/index.html>`__ section for more details.

HttpClient
----------

A simple and barebones `Finagle Client <https://twitter.github.io/finagle/guide/Clients.html>`__ for
making HTTP requests.

HTTP
----

Base library for HTTP services. See the `HTTP Server Definition <../http/server.html>`__ section for
more information.

Thrift
------

Base library for Thrift services. See the `Thrift Basics <../thrift/basics.html>`__ and `Thrift
Server Definition <../thrift/server.html>`__ sections for more details.

Utils
-----

A collection of framework utilities that are specifically useful in HTTP or Thrift.

Other
-----

- `inject-thrift-client-http-mapper` - a library which bridges `inject-thrift-client` and `http`
  libraries to provide an exception mapper for mapping thrift exceptions to HTTP responses.

.. |c.t.app.App| replace:: `util-app App`
.. _c.t.app.App: https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala

.. |c.t.server.TwitterServer| replace:: ``c.t.server.TwitterServer``
.. _c.t.server.TwitterServer: https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala

.. |TwitterServer| replace:: `TwitterServer`
.. _TwitterServer: https://github.com/twitter/twitter-server/blob/develop/server/src/main/scala/com/twitter/server/TwitterServer.scala
