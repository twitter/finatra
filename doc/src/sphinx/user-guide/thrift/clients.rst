.. _thrift_clients:

Communicate with a Thrift Service
=================================

Finatra provides support for integrating with `Scrooge <https://twitter.github.io/scrooge>`__-generated
`Finagle Thrift clients <https://twitter.github.io/finagle/guide/Clients.html>`__.

As mentioned in the Scrooge `Finagle Integration <https://twitter.github.io/scrooge/Finagle.html>`__
documentation, users have three API choices for building an interface to a Finagle Thrift client —
``ServicePerEndpoint``, ``ReqRepServicePerEndpoint``, and ``MethodPerEndpoint``.

The ``ServicePerEndpoint`` interface is a collection of Finagle |Services|_ where each Thrift method
is represented as a Finagle `Service[method.Args, method.SuccessType]`. By their nature of being
|Services|_, the methods are composable with Finagle's |Filters|_.

The ``ReqRepServicePerEndpoint`` interface is also a collection of Finagle |Services|_, however
methods are |Services|_ from a |c.t.scrooge.Request|_ to a |c.t.scrooge.Response|_, e.g.,
`Service[Request[method.Args], Response[method.SuccessType]]`. These envelope types allow for the
passing of header information between clients and servers when using the `com.twitter.finagle.ThriftMux`
protocol.

Getting Started
---------------

To start, add a dependency on the Finatra `inject-thrift-client <http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20a%3A%22inject-thrift-client_2.12%22>`__ library.

E.g., with sbt:

.. parsed-literal::

    "com.twitter" %% "inject-thrift-client" % "\ |release|\ ",

|ServicePerEndpointModule|_
----------------------------------------------

.. note:: Currently the |c.t.inject.thrift.modules.ServicePerEndpointModule|_ only supports the
ThriftMux protocol.

The |c.t.inject.thrift.modules.ServicePerEndpointModule|_ allows for configuration of a Finagle
Thrift client using the Finagle ThriftMux `MethodBuilder <https://twitter.github.io/finagle/guide/MethodBuilder.html>`__
integration.

Users have the option to configure a `service-per-endpoint`, i.e., ``ServicePerEndpoint`` or
``ReqRepServicePerEndpoint`` interface of a Thrift client and the |c.t.inject.thrift.modules.ServicePerEndpointModule|_
provides bindings for both the chosen `service-per-endpoint` interface and the ``MethodPerEndpoint``
interface. In this case, the ``MethodPerEndpoint`` interface functions as a thin wrapper the
configured `service-per-endpoint`.

The choice to interact with Finagle |Services|_ when calling the configured Thrift client or to
use the Thrift method interface is up to you. You will have access to both in the object graph when
implementing a |c.t.inject.thrift.modules.ServicePerEndpointModule|_.

To create a new client, first create a new `TwitterModule <../getting-started/modules.html>`_ which
extends |c.t.inject.thrift.modules.ServicePerEndpointModule|_:

.. code:: scala

    import com.twitter.inject.thrift.modules.ServicePerEndpointModule
    import com.twitter.myservice.thriftscala.MyService

    object MyServiceModule
      extends ServicePerEndpointModule[MyService.ServicePerEndpoint, MyService.MethodPerEndpoint] {

      override val dest = "flag!myservice-thrift-service"
      override val label = "myservice-thrift-client"
    }

The |ServicePerEndpointModule|_ implementation must be typed to a Scrooge-generated `service-per-endpoint`
and the ``MethodPerEndpoint``. Users can choose either the `MyService.ServicePerEndpoint` interface
(as in the above example) or the `MyService.ReqRepServicePerEndpoint` interface depending on their
requirements:

.. code:: scala

    object MyServiceModule
      extends ServicePerEndpointModule[MyService.ReqRepServicePerEndpoint, MyService.MethodPerEndpoint] {
      ...

At a minimum, to use the |c.t.inject.thrift.modules.ServicePerEndpointModule|_, a ThriftMux
`client label <https://twitter.github.io/finagle/guide/Clients.html#observability>`__ and String
`dest <https://twitter.github.io/finagle/guide/Names.html>`__ MUST also be specified.

See `here <https://twitter.github.io/finagle/guide/Clients.html>`__ for more information on Finagle
clients.

Configuration
~~~~~~~~~~~~~

The |ServicePerEndpointModule|_ intends to allow users to configure ThriftMux client
semantics and apply filters per-method via the |c.t.inject.thrift.ThriftMethodBuilder|_ which is a
thin wrapper over the Finagle ThriftMux `MethodBuilder <https://twitter.github.io/finagle/guide/MethodBuilder.html>`__.

To configure per-method semantics, override and provide an implementation for the
`ServicePerEndpointModule#configureServicePerEndpointModule` method. E.g.,

.. code:: scala

    import com.twitter.inject.Injector
    import com.twitter.inject.thrift.ThriftMethodBuilderFactory
    import com.twitter.inject.thrift.modules.ServicePerEndpointModule
    import com.twitter.myservice.thriftscala.MyService

    object MyServiceModule
      extends ServicePerEndpointModule[MyService.ServicePerEndpoint, MyService.MethodPerEndpoint] {

      override val dest = "flag!myservice-thrift-service"
      override val label = "myservice-thrift-client"

      override def configureServicePerEndpoint(
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
client-side `ServicePerEndpoint` provides a `withXXXX` function over every defined Thrift method that
allows users to replace the current implementation of the method with a new implementation. The
replacement must still be a correctly-typed Finagle `Service`.

In the above example we replace the methods with implementations built up from a combination of
`MethodBuilder <https://twitter.github.io/finagle/guide/MethodBuilder.html>`__ functionality and
arbitrary filters ending with a call to `ThriftMethodBuilder#service` which materializes the
resultant `Service`.

Note that `TypeAgnostic <https://github.com/twitter/finagle/blob/f61b6f99c7d108b458d5adcb9891ff6ddda7f125/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala#L203>`__
Finagle |Filters|_ can also be applied "globally" across the all methods of a `ServicePerEndpoint`
interface by calling ``ServicePerEndpoint#filtered``.

For example, to apply a set of `TypeAgnostic <https://github.com/twitter/finagle/blob/f61b6f99c7d108b458d5adcb9891ff6ddda7f125/finagle-core/src/main/scala/com/twitter/finagle/Filter.scala#L203>`__
Finagle Filters to a `ServicePerEndpoint`:

.. code:: scala

    import com.twitter.inject.Injector
    import com.twitter.inject.thrift.ThriftMethodBuilderFactory
    import com.twitter.inject.thrift.modules.ServicePerEndpointModule
    import com.twitter.myservice.thriftscala.MyService

    object MyServiceModule
      extends ServicePerEndpointModule[MyService.ServicePerEndpoint, MyService.MethodPerEndpoint] {

      override val dest = "flag!myservice-thrift-service"
      override val label = "myservice-thrift-client"

      override def configureServicePerEndpoint(
        injector: Injector,
        builder: ThriftMethodBuilderFactory[MyService.ServicePerEndpoint],
        servicePerEndpoint: MyService.ServicePerEndpoint
      ): MyService.ServicePerEndpoint = {

        servicePerEndpoint
          .filtered(???)
      }
    }

This can be combined with the per-method configuration as well.

.. admonition:: More Information on `Modules <../getting-started/modules.html>`__:

      Module `best practices <../getting-started/modules.html#best-practices>`__
      and `depending on other modules <../getting-started/modules.html#modules-depending-on-other-modules>`__.

Bindings
~~~~~~~~

When included in a server's `module <../getting-started/modules.html>`__ list, an implementation
of the |ServicePerEndpointModule|_ will provide bindings to both
`MyService.ServicePerEndpoint <https://twitter.github.io/scrooge/Finagle.html#id2>`__ (or
`MyService.ReqRepServicePerEndpoint <https://twitter.github.io/scrooge/Finagle.html#id3>`__) **and**
`MyService.MethodPerEndpoint <https://twitter.github.io/scrooge/Finagle.html#id1>`__.

For example, given the following |ServicePerEndpointModule|_ implementation:

.. code:: scala

    import com.twitter.inject.thrift.modules.ServicePerEndpointModule
    import com.twitter.myservice.thriftscala.MyService

    object MyServiceModule
      extends ServicePerEndpointModule[MyService.ServicePerEndpoint, MyService.MethodPerEndpoint] {

      override val dest = "flag!myservice-thrift-service"
      override val label = "myservice-thrift-client"
    }

This means that both `MyService.ServicePerEndpoint` and `MyService.MethodPerEndpoint` will be
injectable. Which to use is dependent on your use-case.

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

For more information, see the `Finagle Integration <https://twitter.github.io/scrooge/Finagle.html>`__
section of the `Scrooge <https://twitter.github.io/scrooge/index.html>`__ documentation.

More detailed examples are available in the integration tests:

- |DoEverythingServicePerEndpointModuleFeatureTest|_ and
- |DoEverythingReqRepServicePerEndpointModuleFeatureTest|_

which test over two different implementations of a |ServicePerEndpointModule|_:

- |GreeterServicePerEndpointModule|_ and
- |GreeterReqRepServicePerEndpointModule|_ respectively.

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

.. |c.t.inject.thrift.modules.ServicePerEndpointModule| replace:: `c.t.inject.thrift.modules.ServicePerEndpointModule`
.. _c.t.inject.thrift.modules.ServicePerEndpointModule: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/ServicePerEndpointModule.scala

.. |ServicePerEndpointModule| replace:: `ServicePerEndpointModule`
.. _ServicePerEndpointModule: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/main/scala/com/twitter/inject/thrift/modules/ServicePerEndpointModule.scala

.. |DoEverythingServicePerEndpointModuleFeatureTest| replace:: `DoEverythingServicePerEndpointModuleFeatureTest`
.. _DoEverythingServicePerEndpointModuleFeatureTest: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/scala/com/twitter/inject/thrift/DoEverythingServicePerEndpointModuleFeatureTest.scala

.. |GreeterServicePerEndpointModule| replace:: `GreeterServicePerEndpointModule`
.. _GreeterServicePerEndpointModule: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/scala/com/twitter/inject/thrift/integration/serviceperendpoint/GreeterServicePerEndpointModule.scala

.. |DoEverythingReqRepServicePerEndpointModuleFeatureTest| replace:: `DoEverythingReqRepServicePerEndpointModuleFeatureTest`
.. _DoEverythingReqRepServicePerEndpointModuleFeatureTest: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/scala/com/twitter/inject/thrift/DoEverythingReqRepServicePerEndpointModuleFeatureTest.scala

.. |GreeterReqRepServicePerEndpointModule| replace:: `GreeterReqRepServicePerEndpointModule`
.. _GreeterReqRepServicePerEndpointModule: https://github.com/twitter/finatra/blob/develop/inject/inject-thrift-client/src/test/scala/com/twitter/inject/thrift/integration/reqrepserviceperendpoint/GreeterReqRepServicePerEndpointModule.scala
