.. _framework:

Understanding the Codebase
==========================

Finatra is divided into three main parts: ``inject``, ``http``, and ``thrift``. The `inject` part of the framework contains all of the base functionality for integration of the Guice dependency injection framework with `TwitterUtil <https://twitter.github.io/util/>`__'s `c.t.app.App <https://twitter.github.io/util/docs/com/twitter/app/App.html>`__ and `TwitterServer <https://twitter.github.io/twitter-server/>`__'s `c.t.server.TwitterServer`. 

Modules in the `inject` portion of the Finatra framework constitute the core of the framework, are in the ``com.twitter.inject`` package namespace, and only depend on each other or outside dependencies (except in testing). The `inject` layer can be thought of as dealing with concerns that are common to the framework or that span both http and thrift services.

Everything else in the Finatra codebase is in the ``com.twitter.finatra`` package namespace and has dependencies on the `inject` libraries as well as other libraries.

Inject
------

Modules in `inject` include:

- `inject-core` - core library which defines the `TwitterModule <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModule.scala>`__, `Injector <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Injector.scala>`__, and the framework `TwitterModuleFlags <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/TwitterModuleFlags.scala>`__ for defining injectable `TwitterUtil <https://github.com/twitter/util>`__ `Flags <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/Flag.scala>`__.
- `inject-modules` - core library which defines framework modules.
- `inject-app` - core integration with `c.t.app.App <https://twitter.github.io/util/docs/com/twitter/app/App.html>`__
- `inject-server` - core integration with `c.t.server.TwitterServer`.
- `inject-slf4j` - core library integration with the `SLF4J <http://www.slf4j.org/manual.html>`__ API.
- `inject-request-scope` - core library for integrating Guice's `Request Scope` with `TwitterUtil <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala>`__ `c.t.util.Future`. See the `Request Scope <../http/filters.html#request-scope>`__ section for more information.
- `inject-thrift` - core library for thrift utilities useful for both clients and server.
- `inject-thrift-client` - core library for supporting creation of a Thrift `Finagle Client <https://twitter.github.io/finagle/guide/Clients.html>`__.
- `inject-utils` - core library which provides a collection of useful utilities, including implicit conversions, exceptions and futures handling and retries.

Creating an injectable App or TwitterServer
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you simply want to create an injectable `c.t.app.App` or `c.t.server.TwitterServer`, you can directly depend on libraries in `inject` namely: `inject-app` or `inject-server` respectively.

To use the framework to create an injectable App:

.. code:: scala

	import com.twitter.inject.App
	import com.twitter.inject.Logging

	object MyAppMain extends MyApp

	class MyApp 
	  extends App 
	  with Logging  {

	  override val modules = Seq(
	    MyModule1)

	  override protected def run(): Unit = {
	    // App logic goes here.
	  }
	}

Similarily, to create an injectable TwitterServer:

.. code:: scala

	import com.twitter.inject.Logging
	import com.twitter.inject.server.TwitterServer

	object MyTwitterServerMain extends MyTwitterServer

	class MyTwitterServer 
	  extends TwitterServer 
	  with Logging {

	  override val modules = Seq(
	    MyModule1)

	  override protected def start(): Unit = {
	    // Server logic goes here. It is important to remember to NOT BLOCK this method.
	  }
	}

Note:
^^^^^

It is important to remember that the Finatra framework handles the `lifecycle <lifecycle.html>`__ for you for both a `TwitterUtil <https://twitter.github.io/util/>`__'s `c.t.app.App` and a `TwitterServer <https://twitter.github.io/twitter-server/>`__ `c.t.server.TwitterServer` (see `here <https://github.com/twitter/finatra/blob/develop/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala#L53>`__ and `here <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/main/scala/com/twitter/inject/server/TwitterServer.scala#L127>`__), thus you do not have access to the `main` method. Instead, any logic should be contained in either the `c.t.inject.app.App#run` or `c.t.inject.server.TwitterServer#start` method.

See the `Lifecycle <lifecycle.html>`__ section for more information.

Jackson
-------

Finatra's integration with `jackson-module-scala <https://github.com/FasterXML/jackson-module-scala>`__ for JSON support. See the `Jackson Integration <../json/index.html>`__ section for more details.

HttpClient
----------

A simple and barebones `Finagle Client <https://twitter.github.io/finagle/guide/Clients.html>`__ for making HTTP requests.

HTTP
----

Base library for HTTP services. See the `HTTP Server Definition <../http/server.html>`__ section for more information.

Thrift
------

Base library for Thrift services. See the `Thrift Basics <../thrift/basics.html>`__ and `Thrift Server Definition <../thrift/server.html>`__ sections for more details.

Utils
-----

A collection of framework utilities that are specifically useful in HTTP or Thrift.

Other
-----

- `slf4j` - provides support for bridging Logback's `Mapped Diagnostic Context <http://logback.qos.ch/manual/mdc.html>`__ with Finagle. See `Mapped Diagnostic Context Filter <../logging/logback.html#mapped-diagnostic-context-filter>`__ section for more information.
- `inject-thrift-client-http-mapper` - a library which bridges `inject-thrift-client` and `http` libraries to provide an exception mapper for mapping thrift exceptions to HTTP responses.
