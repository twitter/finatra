.. _http_message_body:

Message Body Components
=======================

Message body components specify how to parse an incoming Finagle
`HTTP request <https://github.com/twitter/finagle/blob/develop/finagle-base-http/src/main/scala/com/twitter/finagle/http/Request.scala>`__
into a model or domain object ("message body reader") and how to transform a given  type `T` into a
Finagle `HTTP response <https://github.com/twitter/finagle/blob/develop/finagle-base-http/src/main/scala/com/twitter/finagle/http/Response.scala>`__
("message body writer").

.. note:: Classes in *internal* packages (i.e., `com.twitter.finatra.http.internal`) are not expected to be used directly and no guarantee is
          given as to the stability of their interfaces. Please do not use these classes directly.

Message Body Readers
--------------------

Message body readers can be registered through the |HttpRouter|_ much like adding a
`Controller <controllers.html>`__ or a `Filter <filters.html>`__.

.. code:: scala

    import DoEverythingModule
    import ExampleController
    import com.twitter.finatra.http.routing.HttpRouter
    import com.twitter.finatra.http.{Controller, HttpServer}

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

      override val modules = Seq(
        DoEverythingModule)

      override def configureHttp(router: HttpRouter): Unit = {
        router
          .add[ExampleController]
          .register[MyModelObjectMessageBodyReader]
      }
    }

Message body readers are used to convert an incoming Finagle HTTP request into a type `T` which is
specified as the input type for a route callback.

E.g., a controller with a route specified:

.. code:: scala

    import com.twitter.finatra.http.Controller

    class ExampleController extends Controller {

      get("/") { model: MyModelObject =>
        ...
      }

will trigger the framework to search for a registered message body reader that can convert the incoming
Finagle HTTP request to `MyModelObject`. If a message body reader for the `MyModelObject` type
cannot be found the `DefaultMessageBodyReader` implementation configured in the |MessageBodyManager|_
will be used.

|DefaultMessageBodyReader|_
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The framework provides a default message body reader implementation: |c.t.finatra.http.internal.marshalling.DefaultMessageBodyReaderImpl|_
which is invoked when a more specific message body reader cannot be found to convert an incoming
Finagle HTTP request.

The `DefaultMessageBodyReaderImpl` parses an incoming Finagle HTTP request
`body <https://github.com/twitter/finagle/blob/f61b6f99c7d108b458d5adcb9891ff6ddda7f125/finagle-base-http/src/main/scala/com/twitter/finagle/http/Message.scala#L440>`__
as JSON, marshalling it into the given callback input type using the `HttpServer <https://github.com/twitter/finatra/blob/712edf91c0361fd9907deaef06e0bd61384f6a7e/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala#L81>`__
`configured <../json/index.html#configuration>`__ `FinatraObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala>`__
and is the basis of the `JSON integration with routing <../json/routing.html>`_.

This default behavior is overridable. See the `c.t.finatra.http.modules.MessageBodyModule` 
`section <#c-t-finatra-http-modules-messagebodymodule>`__ for more information on how to 
provide a different `DefaultMessageBodyReader` implementation.

Message Body Writers
--------------------

Like message body readers, writers can be registered through the |HttpRouter|_ -- again like adding
a `Controller <controllers.html>`__ or a `Filter <filters.html>`__.

.. code:: scala

    import DoEverythingModule
    import ExampleController
    import com.twitter.finatra.http.routing.HttpRouter
    import com.twitter.finatra.http.{Controller, HttpServer}

    object ExampleServerMain extends ExampleServer

    class ExampleServer extends HttpServer {

     override val modules = Seq(
       DoEverythingModule)

     override def configureHttp(router: HttpRouter): Unit = {
       router
         .add[ExampleController]
         .register[MyModelObjectMessageBodyReader]
         .register[MyModelObjectMessageBodyWriter]
     }
    }

Message body writers are used to specify conversion from a type `T` to a Finagle HTTP response. This
can be for the purpose of informing the framework how to render the return type of a route callback
or how to render a type passed as a body to a function in the |c.t.finatra.http.response.ResponseBuilder|_.

E.g., a controller with a route specified:

.. code:: scala

    import com.twitter.finagle.http.Request
    import com.twitter.finatra.http.Controller

    class ExampleController extends Controller {

      get("/") { request: Request =>
        ...
        MyRenderableObjectType(
          id = "1",
          name = "John Doe",
          description = "A renderable return")
      }

will trigger the framework to search for a registered message body writer that can convert the
`MyRenderableObjectType` type into a Finagle HTTP response. If a message body writer for the
`MyRenderableObjectType` type cannot be found the `DefaultMessageBodyWriter` implementation
configured in the |MessageBodyManager|_ will be used.

|DefaultMessageBodyWriter|_
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The framework provides a default message body writer implementation: |c.t.finatra.http.internal.marshalling.DefaultMessageBodyWriterImpl|_
which is invoked when a more specific message body writer cannot be found to convert given type `T`
into a Finagle HTTP response.

The `DefaultMessageBodyWriterImpl` converts any non-primitive type to a `application/json` content-type
response and a JSON representation of the type using the `HttpServer <https://github.com/twitter/finatra/blob/712edf91c0361fd9907deaef06e0bd61384f6a7e/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala#L81>`__ 
`configured <../json/index.html#configuration>`__ `FinatraObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala>`__
to convert the type to JSON.

For primitive and `wrapper <https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/ClassUtils.html#wrapperToPrimitive(java.lang.Class)>`__
types, the default writer implementation will render a `plain/text` content-type response using the
type's `toString` value.

This default behavior is overridable. See the `c.t.finatra.http.modules.MessageBodyModule` 
`section <#c-t-finatra-http-modules-messagebodymodule>`__ for more information on how to 
provide a different `DefaultMessageBodyWriter` implementation.

|MessageBodyManager|_
---------------------

The |MessageBodyManager|_ registers message body components. Generally, you will not need to interact
directly with the manager as a DSL for registration of components is provided by the |HttpRouter|_
(which uses the |MessageBodyManager|_ underneath).

`c.t.finatra.http.modules.MessageBodyModule`
--------------------------------------------

The |DefaultMessageBodyReader|_, and the |DefaultMessageBodyWriter|_ are provided by the framework
via the |c.t.finatra.http.modules.MessageBodyModule|_.

To override the framework defaults, create a `TwitterModule <../getting-started/modules.html>`__
which provides customized implementations for the default reader and writer.

Set this module by overriding the `protected def messageBodyModule` in your server.

.. code:: scala

    class ExampleServer extends HttpServer {

      override def messageBodyModule = MyCustomMessageBodyModule

      override def configureHttp(router: HttpRouter): Unit = {
        ...
      }
    }


If your module is defined as a class, you would pass an instance of the
class, e.g.,

.. code:: scala

    override def messageBodyModule = new MyCustomMessageBodyModule

See `Framework Modules <server.html#framework-modules>`__ for more information.

`Mustache <https://mustache.github.io/>`__ Support 
--------------------------------------------------

`Mustache <https://mustache.github.io/>`__ support is provided through a combination of the
|c.t.finatra.http.modules.MessageBodyModule|_ and a specific `Mustache <https://mustache.github.io/>`__
message body writer.

Finatra provides the |c.t.finatra.http.internal.marshalling.mustache.MustacheMessageBodyWriter|_ which
transforms either a |c.t.finatra.http.marshalling.MessageBodyComponent|_ or an object annotated with
the |@Mustache|_ annotation. The transformation is performed using a referenced
`Mustache <https://mustache.github.io/>`__ template specified by either the component configuration
or as a parameter configured in the |@Mustache|_ annotation.

See the |MessageBodyManager#addByAnnotation|_ and |MessageBodyManager#addByComponentType|_ methods
for  adding an annotated `Mustache <https://mustache.github.io/>`__ view to the |MessageBodyManager|_
and adding a `MessageBodyComponent` by type to the |MessageBodyManager|_ which will instantiate an
instance of the type via the injector.

For examples of how to use the Finatra `Mustache <https://mustache.github.io/>`__ support, please
see the Finatra |web-dashboard|_ example and the |MustacheController|_ used in integration tests.

To better understand how `Mustache <https://mustache.github.io/>`__ templates are found, please see
|MustacheTemplateLookup|_ and the corresponding |MustacheTemplateLookupTest|_. 

For more information on referencing files in Finatra, see the 
`Working with Files <../files/index.html>`__ section.

.. |HttpRouter| replace:: `HttpRouter`
.. _HttpRouter: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala

.. |DefaultMessageBodyReader| replace:: ``DefaultMessageBodyReader``
.. _DefaultMessageBodyReader: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/DefaultMessageBodyReader.scala

.. |c.t.finatra.http.internal.marshalling.DefaultMessageBodyReaderImpl| replace:: `c.t.finatra.http.internal.marshalling.DefaultMessageBodyReaderImpl`
.. _c.t.finatra.http.internal.marshalling.DefaultMessageBodyReaderImpl: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/DefaultMessageBodyReaderImpl.scala

.. |DefaultMessageBodyWriter| replace:: ``DefaultMessageBodyWriter``
.. _DefaultMessageBodyWriter: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/DefaultMessageBodyWriter.scala

.. |c.t.finatra.http.internal.marshalling.DefaultMessageBodyWriterImpl| replace:: `c.t.finatra.http.internal.marshalling.DefaultMessageBodyWriterImpl`
.. _c.t.finatra.http.internal.marshalling.DefaultMessageBodyWriterImpl: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/DefaultMessageBodyWriterImpl.scala

.. |c.t.finatra.http.response.ResponseBuilder| replace:: `c.t.finatra.http.response.ResponseBuilder`
.. _c.t.finatra.http.response.ResponseBuilder: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/response/ResponseBuilder.scala

.. |MessageBodyManager| replace:: `MessageBodyManager`
.. _MessageBodyManager: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/MessageBodyManager.scala

.. |MessageBodyManager#addByAnnotation| replace:: `MessageBodyManager#addByAnnotation`
.. _MessageBodyManager#addByAnnotation: https://github.com/twitter/finatra/blob/6e09e95b95b20d2599a6210dfa0ce4c82dbe636b/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/MessageBodyManager.scala#L54

.. |MessageBodyManager#addByComponentType| replace:: `MessageBodyManager#addByComponentType`
.. _MessageBodyManager#addByComponentType: https://github.com/twitter/finatra/blob/6e09e95b95b20d2599a6210dfa0ce4c82dbe636b/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/MessageBodyManager.scala#L60

.. |c.t.finatra.http.modules.MessageBodyModule| replace:: `c.t.finatra.http.modules.MessageBodyModule`
.. _c.t.finatra.http.modules.MessageBodyModule:  https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/modules/MessageBodyModule.scala

.. |c.t.finatra.http.modules.MustacheModule| replace:: `c.t.finatra.http.modules.MustacheModule`
.. _c.t.finatra.http.modules.MustacheModule:

.. |c.t.finatra.http.internal.marshalling.mustache.MustacheMessageBodyWriter| replace:: `c.t.finatra.http.internal.marshalling.mustache.MustacheMessageBodyWriter`
.. _c.t.finatra.http.internal.marshalling.mustache.MustacheMessageBodyWriter: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/mustache/MustacheMessageBodyWriter.scala

.. |c.t.finatra.http.marshalling.MessageBodyComponent| replace:: `c.t.finatra.http.marshalling.MessageBodyComponent`
.. _c.t.finatra.http.marshalling.MessageBodyComponent: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/MessageBodyComponent.scala

.. |@Mustache| replace:: ``@Mustache``
.. _@Mustache: https://github.com/twitter/finatra/blob/develop/http/src/main/java/com.twitter.finatra.http.response.Mustache.java

.. |web-dashboard| replace:: `web-dashboard`
.. _web-dashboard: https://github.com/twitter/finatra/tree/develop/examples/web-dashboard

.. |MustacheController| replace:: `MustacheController`
.. _MustacheController: https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/controllers/MustacheController.scala

.. |MustacheTemplateLookup| replace:: `MustacheTemplateLookup`
.. _MustacheTemplateLookup: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/mustache/MustacheTemplateLookup.scala

.. |MustacheTemplateLookupTest| replace:: `MustacheTemplateLookupTest`
.. _MustacheTemplateLookupTest: https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/internal/marshalling/mustache/MustacheTemplateLookupTest.scala
