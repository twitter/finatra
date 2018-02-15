.. _thrift_clients:

Communicate with a Thrift Service
=================================

Finatra provides support for integrating with `Scrooge <https://twitter.github.io/scrooge>`__-generated
`Finagle Thrift clients <https://twitter.github.io/finagle/guide/Clients.html>`__.

As mentioned in the Scrooge `Finagle Integration <https://twitter.github.io/scrooge/Finagle.html>`__
documentation, users have three API choices for building an interface to a Finagle Thrift client —
``ServicePerEndpoint``, ``ReqRepServicePerEndpoint``, and ``MethodPerEndpoint``.

The ``MethodPerEndpoint`` interface is a collection of methods which return `Futures`.

The ``ServicePerEndpoint`` interface is a collection of Finagle |Services|_ where each Thrift method
is represented as a Finagle `Service <https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Service.scala>`__
of `ThriftMethod.Args` to `ThriftMethod.SuccessType`:

.. code:: scala

  Service[method.Args, method.SuccessType]

By their nature of being |Services|_, the methods are composable with Finagle's |Filters|_.

The ``ReqRepServicePerEndpoint`` interface is also a collection of Finagle |Services|_, however
methods are |Services|_ from a |c.t.scrooge.Request|_ to a |c.t.scrooge.Response|_, e.g.,

.. code:: scala

  import com.twitter.scrooge.{Request, Response}

  Service[Request[method.Args], Response[method.SuccessType]]

These envelope types allow for the passing of header information between clients and servers when
using the Finagle `ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__
protocol.

Getting Started
---------------

To start, add a dependency on the Finatra `inject-thrift-client <http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20a%3A%22inject-thrift-client_2.12%22>`__ library.

E.g., with sbt:

.. parsed-literal::

    "com.twitter" %% "inject-thrift-client" % "\ |release|\ ",

|ThriftMethodBuilderClientModule|_
----------------------------------

.. note:: The |c.t.inject.thrift.modules.ThriftMethodBuilderClientModule|_ only supports the
          `ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__
          protocol.

The |c.t.inject.thrift.modules.ThriftMethodBuilderClientModule|_ allows for configuration of a Finagle
Thrift client using the Finagle `ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__
`MethodBuilder <https://twitter.github.io/finagle/guide/MethodBuilder.html>`__ integration.

Users have the option to configure a `service-per-endpoint`, i.e., ``ServicePerEndpoint`` or
``ReqRepServicePerEndpoint`` interface of the Thrift client and the |c.t.inject.thrift.modules.ThriftMethodBuilderClientModule|_
provides bindings for both the chosen `service-per-endpoint` interface and the ``MethodPerEndpoint``
interface. In this case, the ``MethodPerEndpoint`` interface functions as a thin wrapper over the
configured `service-per-endpoint`.

The choice to interact with Finagle |Services|_ when calling the configured Thrift client or to
use the Thrift method interface is up to you. You will have access to both in the object graph when
implementing a |ThriftMethodBuilderClientModule|_.

To create a new client, first create a new `TwitterModule <../getting-started/modules.html>`_ which
extends |c.t.inject.thrift.modules.ThriftMethodBuilderClientModule|_:

.. code:: scala

    import com.twitter.finagle.Budget
    import com.twitter.inject.thrift.modules.ThriftMethodBuilderClientModule
    import com.twitter.myservice.thriftscala.MyService
    import com.twitter.util.{Duration, Monitor}

    object MyServiceModule
      extends ThriftMethodBuilderClientModule[
        MyService.ServicePerEndpoint,
        MyService.MethodPerEndpoint] {

      override val dest: String = "flag!myservice-thrift-service"
      override val label: String = "myservice-thrift-client"

      override val sessionAcquisitionTimeout: Duration = ???
      override val requestTimeout: Duration = ???
      override val retryBudget: Budget = ???
      override val monitor: Monitor = ???
    }

The |ThriftMethodBuilderClientModule|_ implementation must be typed to a Scrooge-generated `service-per-endpoint`
and the ``MethodPerEndpoint``. Users can choose either the `MyService.ServicePerEndpoint` interface
(as in the above example) or the `MyService.ReqRepServicePerEndpoint` interface depending on their
requirements:

.. code:: scala

    object MyServiceModule
      extends ThriftMethodBuilderClientModule[
        MyService.ReqRepServicePerEndpoint,
        MyService.MethodPerEndpoint] {
      ...

At a minimum, to use the |c.t.inject.thrift.modules.ThriftMethodBuilderClientModule|_, a
`ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__
`client label <https://twitter.github.io/finagle/guide/Clients.html#observability>`__ and a String
`dest <https://twitter.github.io/finagle/guide/Names.html>`__ **must** be specified.

Configuration
~~~~~~~~~~~~~

The |ThriftMethodBuilderClientModule|_ intends to allow users to configure
`ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__ client
semantics and apply filters per-method via the |c.t.inject.thrift.ThriftMethodBuilder|_ which is a
thin wrapper over the Finagle `ThriftMux  <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__
`MethodBuilder <https://twitter.github.io/finagle/guide/MethodBuilder.html>`__.

Advanced `ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__ client
configuration can be done by overriding the `ThriftMethodBuilderClientModule#configureThriftMuxClient`
method which allows for ad-hoc `ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__
client configuration.

See `Finagle Client Modules <https://twitter.github.io/finagle/guide/Clients.html#client-modules>`__
for more information on client configuration parameters and their meanings.

Per-Method Configuration
~~~~~~~~~~~~~~~~~~~~~~~~

To configure per-method semantics, override and provide an implementation for the
`ThriftMethodBuilderClientModule#configureServicePerEndpoint` method. E.g.,

.. code:: scala

    import com.twitter.inject.Injector
    import com.twitter.inject.thrift.ThriftMethodBuilderFactory
    import com.twitter.inject.thrift.modules.ThriftMethodBuilderClientModule
    import com.twitter.myservice.thriftscala.MyService

    object MyServiceModule
      extends ThriftMethodBuilderClientModule[
        MyService.ServicePerEndpoint,
        MyService.MethodPerEndpoint] {

      override val dest: String = "flag!myservice-thrift-service"
      override val label: String = "myservice-thrift-client"

      override protected def configureServicePerEndpoint(
        injector: Injector,
        builder: ThriftMethodBuilderFactory[MyService.ServicePerEndpoint],
        servicePerEndpoint: MyService.ServicePerEndpoint
      ): MyService.ServicePerEndpoint = {

        servicePerEndpoint
          .withFoo(
            builder.method(MyService.Foo)
              .withTimeoutPerRequest(???)
              .withTotalTimeout(???)
              .withRetryForClassifier(???)
              .filtered(new MyFooMethodFilter)
              .service)
          .withBar(
            builder.method(MyService.Bar)
              .filtered(new MyTypeAgnosticFilter)
              .withRetryForClassifier(???)
              .service)
      }
    }

In this example we are configuring the given `servicePerEndpoint` by re-implementing the `Foo` and
`Bar` functions using a "builder"-like API. Each `Scrooge <https://twitter.github.io/scrooge>`__-generated
client-side ``ServicePerEndpoint`` provides a `withXXXX` function over every defined Thrift method that
allows users to replace the current implementation of the method with a new implementation. The
replacement must still be a correctly-typed Finagle `Service`.

In the above example we replace the methods with implementations built up from a combination of
`MethodBuilder <https://twitter.github.io/finagle/guide/MethodBuilder.html>`__ functionality and
arbitrary filters ending with a call to `ThriftMethodBuilder#service` which materializes the
resultant `Service[-Req, +Rep]`.

Global Filters
^^^^^^^^^^^^^^

Note that `TypeAgnostic <https://github.com/twitter/finagle/blob/f61b6f99c7d108b458d5adcb9891ff6ddda7f125/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala#L203>`__
Finagle |Filters|_ can also be applied "globally" across the all methods of a ``ServicePerEndpoint``
interface by calling `ServicePerEndpoint#filtered`.

For example, to apply a set of `TypeAgnostic <https://github.com/twitter/finagle/blob/f61b6f99c7d108b458d5adcb9891ff6ddda7f125/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala#L203>`__
Finagle Filters to a ``ServicePerEndpoint``:

.. code:: scala

    import com.twitter.inject.Injector
    import com.twitter.inject.thrift.ThriftMethodBuilderFactory
    import com.twitter.inject.thrift.modules.ThriftMethodBuilderClientModule
    import com.twitter.myservice.thriftscala.MyService

    object MyServiceModule
      extends ThriftMethodBuilderClientModule[MyService.ServicePerEndpoint, MyService.MethodPerEndpoint] {

      override val dest: String = "flag!myservice-thrift-service"
      override val label: String = "myservice-thrift-client"

      override protected def configureServicePerEndpoint(
        injector: Injector,
        builder: ThriftMethodBuilderFactory[MyService.ServicePerEndpoint],
        servicePerEndpoint: MyService.ServicePerEndpoint
      ): MyService.ServicePerEndpoint = {

        servicePerEndpoint
          .filtered(???)
      }
    }

This can be combined with the per-method configuration as well.

ThriftMethodBuilderClientModule Bindings
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When included in a server's `module <../getting-started/modules.html>`__ list, an implementation
of the |ThriftMethodBuilderClientModule|_ will provide bindings to both
`MyService.ServicePerEndpoint <https://twitter.github.io/scrooge/Finagle.html#id2>`__ (or
`MyService.ReqRepServicePerEndpoint <https://twitter.github.io/scrooge/Finagle.html#id3>`__) **and**
`MyService.MethodPerEndpoint <https://twitter.github.io/scrooge/Finagle.html#id1>`__.

For example, given the following |ThriftMethodBuilderClientModule|_ implementation:

.. code:: scala

    import com.twitter.inject.thrift.modules.ThriftMethodBuilderClientModule
    import com.twitter.myservice.thriftscala.MyService

    object MyServiceModule
      extends ThriftMethodBuilderClientModule[
        MyService.ServicePerEndpoint,
        MyService.MethodPerEndpoint] {

      override val dest: String = "flag!myservice-thrift-service"
      override val label: String = "myservice-thrift-client"
    }

This means that both the `MyService.ServicePerEndpoint` and `MyService.MethodPerEndpoint` types will
be injectable. Which to use is dependent on your use-case.

.. code:: scala

    import com.twitter.finagle.http.Request
    import com.twitter.finatra.http.Controller
    import com.twitter.myservice.thriftscala.MyService
    import javax.inject.{Inject, Singleton}

    @Singleton
    class MyDataController @Inject()(
      myService: MyService.MethodPerEndpoint
    ) extends Controller {
      get("/") { request: Request =>
        myService.foo(request.params("data"))
      }
    }

|ThriftClientModule|_
---------------------

.. note:: The |c.t.inject.thrift.modules.ThriftClientModule|_ only supports the
          `ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__
          protocol.

The |c.t.inject.thrift.modules.ThriftClientModule|_ allows for simpler configuration of a Finagle
Thrift client than the |c.t.inject.thrift.modules.ThriftMethodBuilderClientModule|_.

Users have the option of configuring either a ``MethodPerEndpoint`` or the higher-kinded, e.g.,
``MyService[+MM[_]]``, interface of the Thrift client and the |c.t.inject.thrift.modules.ThriftClientModule|_
provides a binding to the chosen interface.

To create a new client, first create a new `TwitterModule <../getting-started/modules.html>`_ which
extends |c.t.inject.thrift.modules.ThriftClientModule|_:

.. code:: scala

    import com.twitter.inject.thrift.modules.ThriftClientModule
    import com.twitter.myservice.thriftscala.MyService

    object MyServiceModule
      extends ThriftClientModule[MyService.MethodPerEndpoint] {

      override val dest: String = "flag!myservice-thrift-service"
      override val label: String = "myservice-thrift-client"
    }

The |ThriftClientModule|_ implementation must be typed to either the Scrooge-generated
``MethodPerEndpoint`` or the ``MyService[+MM[_]]`` Thrift service interface. These interfaces are
semantically equivalent, however there are differences when it comes to some testing features which
have trouble dealing with higher-kinded types (like mocking).

Users can choose either interface depending on their requirements. E.g., to use the ``MyService[+MM[_]]``
interface for `MyService`:

.. code:: scala

    object MyServiceModule
      extends ThriftClientModule[MyService[Future]] {
      ...

At a minimum, to use the |c.t.inject.thrift.modules.ThriftClientModule|_, a
`ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__
`client label <https://twitter.github.io/finagle/guide/Clients.html#observability>`__ and a String
`dest <https://twitter.github.io/finagle/guide/Names.html>`__ **must** be specified.

Configuration
~~~~~~~~~~~~~

The |ThriftClientModule|_ intends to allow users to easily configure common parameters of a
`ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__ client.

.. code:: scala

    import com.twitter.finagle.Budget
    import com.twitter.inject.Injector
    import com.twitter.inject.thrift.ThriftMethodBuilderFactory
    import com.twitter.inject.thrift.modules.ThriftClientModule
    import com.twitter.myservice.thriftscala.MyService
    import com.twitter.util.{Duration, Monitor}

    object MyServiceModule
      extends ThriftClientModule[MyService.MethodPerEndpoint] {

      override val dest: String = "flag!myservice-thrift-service"
      override val label: String = "myservice-thrift-client"

      override val sessionAcquisitionTimeout: Duration = ???

      override val requestTimeout: Duration = ???

      override val retryBudget: Budget = ???

      override val monitor: Monitor = ???

Advanced `ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__
configuration can be done by overriding the `ThriftClientModule#configureThriftMuxClient`
method which allows for ad-hoc `ThriftMux <https://twitter.github.io/finagle/docs/com/twitter/finagle/ThriftMux$>`__
client configuration.

See `Finagle Client Modules <https://twitter.github.io/finagle/guide/Clients.html#client-modules>`__
for more information on client configuration parameters and their meanings.

ThriftClientModule Bindings
~~~~~~~~~~~~~~~~~~~~~~~~~~~

When included in a server's `module <../getting-started/modules.html>`__ list, an implementation
of the |ThriftClientModule|_ will provide a binding of the specified type param to the object graph.
Either `MyService.MethodPerEndpoint <https://twitter.github.io/scrooge/Finagle.html#id1>`__ or
`MyService[Future]`.

For example, given the following |ThriftClientModule|_ implementation:

.. code:: scala

   import com.twitter.inject.thrift.modules.ThriftClientModule
   import com.twitter.myservice.thriftscala.MyService

   object MyServiceModule
     extends ThriftClientModule[MyService.MethodPerEndpoint] {

     override val dest: String = "flag!myservice-thrift-service"
     override val label: String = "myservice-thrift-client"
   }

This means that the `MyService.MethodPerEndpoint` type will be injectable.

.. code:: scala

   import com.twitter.finagle.http.Request
   import com.twitter.finatra.http.Controller
   import com.twitter.myservice.thriftscala.MyService
   import javax.inject.{Inject, Singleton}

   @Singleton
   class MyDataController @Inject()(
     myService: MyService.MethodPerEndpoint
   ) extends Controller {
     get("/") { request: Request =>
       myService.foo(request.params("data"))
     }
   }

More Information
----------------

.. admonition:: More Information on `Modules <../getting-started/modules.html>`__:

      Module `best practices <../getting-started/modules.html#best-practices>`__
      and `depending on other modules <../getting-started/modules.html#modules-depending-on-other-modules>`__.

For more information on `Scrooge <https://twitter.github.io/scrooge/index.html>`__-generated client
interfaces see the `Finagle Integration <https://twitter.github.io/scrooge/Finagle.html>`__ section
of the `Scrooge <https://twitter.github.io/scrooge/index.html>`__ documentation.

More detailed examples are available in the integration tests:

- |DoEverythingThriftClientModuleFeatureTest|_
- |DoEverythingThriftMethodBuilderClientModuleFeatureTest|_
- |DoEverythingReqRepThriftMethodBuilderClientModuleFeatureTest|_

which test over multiple implementations of a |ThriftClientModule|_ and |ThriftMethodBuilderClientModule|_:

- |EchoThriftClientModules|_
- |GreeterThriftMethodBuilderClientModule|_
- |GreeterReqRepThriftMethodBuilderClientModule|_.

.. |Services| replace:: `Services`
.. _Services: https://twitter.github.io/finagle/guide/ServicesAndFilters.html#services

.. |Filters| replace:: `Filters`
.. _Filters: https://twitter.github.io/finagle/guide/ServicesAndFilters.html#filters

.. |c.t.scrooge.Request| replace:: `c.t.scrooge.Request`
.. _c.t.scrooge.Request: https://github.com/twitter/scrooge/blob/develop/scrooge-core/src/main/scala/com/twitter/scrooge/Request.scala

.. |c.t.scrooge.Response| replace:: `c.t.scrooge.Response`
.. _c.t.scrooge.Response: https://github.com/twitter/scrooge/blob/develop/scrooge-core/src/main/scala/com/twitter/scrooge/Response.scala

.. |c.t.inject.thrift.ThriftMethodBuilder| replace:: ``c.t.inject.thrift.ThriftMethodBuilder``
.. _c.t.inject.thrift.ThriftMethodBuilder: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/ThriftMethodBuilder.scala

.. |c.t.inject.thrift.modules.ThriftMethodBuilderClientModule| replace:: `c.t.inject.thrift.modules.ThriftMethodBuilderClientModule`
.. _c.t.inject.thrift.modules.ThriftMethodBuilderClientModule: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/ThriftMethodBuilderClientModule.scala

.. |ThriftMethodBuilderClientModule| replace:: `ThriftMethodBuilderClientModule`
.. _ThriftMethodBuilderClientModule: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/ThriftMethodBuilderClientModule.scala

.. |ThriftClientModule| replace:: `ThriftClientModule`
.. _ThriftClientModule: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/ThriftClientModule.scala

.. |c.t.inject.thrift.modules.ThriftClientModule| replace:: `c.t.inject.thrift.modules.ThriftClientModule`
.. _c.t.inject.thrift.modules.ThriftClientModule: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/ThriftClientModule.scala

.. |DoEverythingThriftClientModuleFeatureTest| replace:: `DoEverythingThriftClientModuleFeatureTest`
.. _DoEverythingThriftClientModuleFeatureTest: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/scala/com/twitter/inject/thrift/DoEverythingThriftClientModuleFeatureTest.scala

.. |EchoThriftClientModules| replace:: `EchoThriftClientModules`
.. _EchoThriftClientModules: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/scala/com/twitter/inject/thrift/integration/basic

.. |DoEverythingThriftMethodBuilderClientModuleFeatureTest| replace:: `DoEverythingThriftMethodBuilderClientModuleFeatureTest`
.. _DoEverythingThriftMethodBuilderClientModuleFeatureTest: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/scala/com/twitter/inject/thrift/DoEverythingThriftMethodBuilderClientModuleFeatureTest.scala

.. |GreeterThriftMethodBuilderClientModule| replace:: `GreeterThriftMethodBuilderClientModule`
.. _GreeterThriftMethodBuilderClientModule: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/scala/com/twitter/inject/thrift/integration/serviceperendpoint/GreeterThriftMethodBuilderClientModule.scala

.. |DoEverythingReqRepThriftMethodBuilderClientModuleFeatureTest| replace:: `DoEverythingReqRepThriftMethodBuilderClientModuleFeatureTest`
.. _DoEverythingReqRepThriftMethodBuilderClientModuleFeatureTest: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/scala/com/twitter/inject/thrift/DoEverythingReqRepThriftMethodBuilderClientModuleFeatureTest.scala

.. |GreeterReqRepThriftMethodBuilderClientModule| replace:: `GreeterReqRepThriftMethodBuilderClientModule`
.. _GreeterReqRepThriftMethodBuilderClientModule: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/scala/com/twitter/inject/thrift/integration/reqrepserviceperendpoint/GreeterReqRepThriftMethodBuilderClientModule.scala
