.. _http_message_body:

Message Body Components
=======================

Message body components specify how to parse an incoming |c.t.finagle.http.Request|_ into a model or domain
object ("message body reader") or how to transform a given type `T` into a |c.t.finagle.http.Response|_
("message body writer").

`MessageBodyComponent <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/MessageBodyComponent.scala>`_
-------------------------------------------------------------------------------------------------------------------------------------------------------------

A |MessageBodyComponent| is a marker trait to indicate that a class type
is expected to be handled by either a message body reader or writer. That is, the trait allows for
registration of an implementation type of the trait to a reader or writer for either converting a |c.t.finagle.http.Request|_
into the type (via a message body reader) or converting the type into a |c.t.finagle.http.Response|_
(via a message body writer).

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

Message body readers are invoked by the framework to convert an incoming |c.t.finagle.http.Request|_
into a type `T` when `T` is specified as the input type for a `Controller <controllers.html>`__ route
callback or when `T` is passed as a body to a function in the |ResponseBuilder|_.

E.g., a controller with a route specified:

.. code:: scala

    import com.twitter.finatra.http.Controller

    class ExampleController extends Controller {

      get("/") { model: MyModelObject =>
        ...
      }

will trigger the framework to search for a registered message body reader that can convert the incoming
|c.t.finagle.http.Request|_ body contents into `MyModelObject`.

If a message body reader for the `MyModelObject` type cannot be found the `DefaultMessageBodyReader`
implementation configured in the |MessageBodyManager|_ will be used.

`DefaultMessageBodyReader <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/DefaultMessageBodyReader.scala>`_
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The framework provides a default message body reader:
|DefaultMessageBodyReader|_ which is invoked when a more specific
message body reader cannot be found to convert an incoming |c.t.finagle.http.Request|_ body.

The |DefaultMessageBodyReader|_ parses an incoming |c.t.finagle.http.Request|_
`body <https://github.com/twitter/finagle/blob/f61b6f99c7d108b458d5adcb9891ff6ddda7f125/finagle-base-http/src/main/scala/com/twitter/finagle/http/Message.scala#L440>`__
as JSON, marshalling it into the given callback input type using the `HttpServer <https://github.com/twitter/finatra/blob/712edf91c0361fd9907deaef06e0bd61384f6a7e/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala#L81>`__
`configured <../json/index.html#configuration>`__ `FinatraObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala>`__
and is the basis of the `JSON integration with routing <../json/routing.html>`_.

This default behavior is overridable. See the `MessageBodyModule` `section <#id4>`__ for more
information on how to provide a different `DefaultMessageBodyReader` implementation.

Message Body Writers
--------------------

Like message body readers, writers can be registered through the
|HttpRouter|_ -- again like adding a `Controller <controllers.html>`__
or a `Filter <filters.html>`__.

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

Message body writers are used to specify conversion from a type `T` to a |c.t.finagle.http.Response|_.
This can be for the purpose of informing the framework how to render the return type `T` of a route
callback or how to render the type `T` when passed as a body to a function in the
|ResponseBuilder|_.

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
`MyRenderableObjectType` type into a |c.t.finagle.http.Response|_.

If a message body writer for the `MyRenderableObjectType` type cannot be found the
`DefaultMessageBodyWriter` implementation configured in the |MessageBodyManager|_ will be used.

`DefaultMessageBodyWriter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/DefaultMessageBodyWriter.scala>`_
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The framework provides a default message body writer: |DefaultMessageBodyWriter|_
which is invoked when a more specific message body writer cannot be found to convert given type `T`
into a |c.t.finagle.http.Response|_.

The `DefaultMessageBodyWriter` converts any non-primitive type to an `application/json` content type
response and a JSON representation of the type using the
`HttpServer <https://github.com/twitter/finatra/blob/712edf91c0361fd9907deaef06e0bd61384f6a7e/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala#L81>`__
`configured <../json/index.html#configuration>`__ `FinatraObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala>`__
to convert the type to JSON.

For primitive and boxed types, the default writer implementation will render a `plain/text`
content type response using the type's `toString` value.

Again, the default behavior is overridable. See the `c.t.finatra.http.modules.MessageBodyModule`
`section <#c-t-finatra-http-modules-messagebodymodule>`__ for more information on how to 
provide a different `DefaultMessageBodyWriter` implementation.

`@MessageBodyWriter` Annotation
-------------------------------

A message body writer can be invoked on a class that is annotated with a `MessageBodyWriter`
`annotation <https://github.com/twitter/finatra/blob/develop/http-annotations/src/main/java/com/twitter/finatra/http/annotations/MessagebodyWriter.java>`_.
That is, a class which is annotated with an annotation that is itself annotated with `@MessageBodyWriter`.

For example. If you have `MyRenderableObjectMessageBodyWriter` and you want to signal to the framework
to invoke this message body writer when trying to convert a given class to a |c.t.finagle.http.Response|_,
you can create a custom annotation and annotate the class like so:

.. code:: java

    import java.lang.annotation.Retention;
    import java.lang.annotation.Target;

    import com.twitter.finatra.http.annotations.MessageBodyWriter;

    import static java.lang.annotation.ElementType.PARAMETER;
    import static java.lang.annotation.RetentionPolicy.RUNTIME;

    @Target(PARAMETER)
    @Retention(RUNTIME)
    @MessageBodyWriter
    public @interface MyRenderable {}

.. code:: scala

    import MyRenderable

    @MyRenderable
    case class SomeValues(name: String, age: Int, address: String)

You would then create a custom `Module <../getting-started/modules.html>`__ to register the
annotation to your `MyRenderableObjectMessageBodyWriter`. You will need to do this registration in the
`TwitterModule#singletonStartup` lifecycle method which ensures that registration will happen after the
object graph has been created but before the server has started.

.. code:: scala

    import MyRenderable
    import MyRenderableObjectMessageBodyWriter
    import MyRenderableObjectType
    import com.twitter.finatra.http.marshalling.MessageBodyManager
    import com.twitter.inject.{Injector, TwitterModule}

    object MyRenderableObjectMessageBodyModule extends TwitterModule {

      override def singletonStartup(injector: Injector): Unit = {
        val manager = injector.instance[MessageBodyManager]
        manager.addByAnnotation[MyRenderable, MyRenderableObjectMessageBodyWriter]()
        manager.addByComponent[MyRenderableObjectType, MyRenderableObjectMessageBodyWriter]()
      }
    }

In this way, whenever an instance of `SomeValues` (|MessageBodyManager#addByAnnotation|_) or
`MyRenderableObjectType` (|MessageBodyManager#addByComponentType|_) is passed to the
framework to render as a |c.t.finagle.http.Response|_ the `MyRenderableObjectMessageBodyWriter`
will be invoked.

Again, this happens when these types are returned from a route callback or when passed as a body
to a function in the |ResponseBuilder|_.

`MessageBodyManager <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/MessageBodyManager.scala>`_
---------------------------------------------------------------------------------------------------------------------------------------------------------

The |MessageBodyManager|_ registers message body components (readers
and writers). Generally, you will not need to interact directly with the manager because the
|HttpRouter|_ provides a DSL for registration of components to the
bound |MessageBodyManager|_.

`MessageBodyModule <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/modules/MessageBodyModule.scala>`_
---------------------------------------------------------------------------------------------------------------------------------------------------

The |DefaultMessageBodyReader|_, and the |DefaultMessageBodyWriter|_
are provided by the framework via configuration in the |MessageBodyModule|_.

To override the framework defaults, create an instance of a `TwitterModule <../getting-started/modules.html>`__
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

    override val messageBodyModule = new MyCustomMessageBodyModule

See `Framework Modules <server.html#framework-modules>`__ for more information.

.. caution::

    Care should be taken when replacing the framework default `c.t.finatra.http.modules.MessageBodyModule`.
    This module binds the framework `DefaultMessageBodyReader` implementation which is what provides
    the logic for marshalling HTTP request bodies as `JSON into case classes <../json/routing.html>`_
    automatically.

    If you replace the `MessageBodyModule` completely and do not retain the binding of the
    framework `DefaultMessageBodyReader` implementation you will lose this functionality.

    Thus it is recommended that you choose to *extend* the `c.t.finatra.http.modules.MessageBodyModule`
    in order to customize your logic and remember to invoke `super` for overridden methods to ensure
    default behavior is retained if so desired. E.g.,

    .. code:: scala

        import com.twitter.finatra.http.modules.MessageBodyModule
        import com.twitter.inject.Injector

        object MyCustomMessageBodyModule extends MessageBodyModule {

          override def singletonStartup(injector: Injector): Unit = {
            super.singletonStartup(injector)
            ???
          }
        }

    See: `Custom Request Case class <./requests.html#custom-request-case-class>`_ documentation
    for more information on the JSON integration with routing.

`Mustache <https://mustache.github.io/>`__ Support 
--------------------------------------------------

`Mustache <https://mustache.github.io/>`__ support for HTTP servers is provided by the `finatra/http-mustache <https://github.com/twitter/finatra/blob/develop/mustache/src/main/scala/com/twitter/finatra/http-mustache>`_
library.

This library provides the |MustacheMessageBodyWriter|_ which transforms an object into a 
|c.t.finagle.http.Response|_ using a provided Mustache template.

Additionally, the library provides:

- a `MustacheBodyComponent` case class which is a `Mustache <https://mustache.github.io/>`__ specific `MessageBodyComponent`.
- the |@Mustache|_ annotation which is a `MessageBodyWriter` `annotation <#messagebodywriter-annotation>`__.
- and a `MustacheModule` which registers the annotation and the component to the |MustacheMessageBodyWriter|_
  for allowing the framework to automatically handle `MustacheBodyComponent` instances or |@Mustache|_ 
  annotated classes.

The transformation is performed using a referenced `Mustache <https://mustache.github.io/>`__ template
specified by either the `MustacheBodyComponent` configuration or as a parameter configured in
the |@Mustache|_ annotation.

You must include the `MustacheModule` in your server's list of modules in order for the framework
to negotiate rendering of `Mustache <https://mustache.github.io/>`__ templates via `MessageBodyComponents`.

For more information the Finatra's Mustache integration with HTTP see the documentation `here <../mustache/routing.html>`_.

.. |c.t.finagle.http.Request| replace:: `c.t.finagle.http.Request`
.. _c.t.finagle.http.Request: https://github.com/twitter/finagle/blob/develop/finagle-base-http/src/main/scala/com/twitter/finagle/http/Request.scala

.. |c.t.finagle.http.Response| replace:: `c.t.finagle.http.Response`
.. _c.t.finagle.http.Response: https://github.com/twitter/finagle/blob/develop/finagle-base-http/src/main/scala/com/twitter/finagle/http/Response.scala

.. |HttpRouter| replace:: `c.t.finatra.http.routing.HttpRouter`
.. _HttpRouter: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala

.. |DefaultMessageBodyReader| replace:: `c.t.finatra.http.marshalling.DefaultMessageBodyReader`
.. _DefaultMessageBodyReader: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/DefaultMessageBodyReader.scala

.. |DefaultMessageBodyWriter| replace:: `c.t.finatra.http.marshalling.DefaultMessageBodyWriter`
.. _DefaultMessageBodyWriter: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/DefaultMessageBodyWriter.scala

.. |ResponseBuilder| replace:: `c.t.finatra.http.response.ResponseBuilder`
.. _ResponseBuilder: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/response/ResponseBuilder.scala

.. |MessageBodyManager| replace:: `c.t.finatra.http.marshalling.MessageBodyManager`
.. _MessageBodyManager: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/MessageBodyManager.scala

.. |MessageBodyManager#addByAnnotation| replace:: `MessageBodyManager#addByAnnotation`
.. _MessageBodyManager#addByAnnotation: https://github.com/twitter/finatra/blob/6e09e95b95b20d2599a6210dfa0ce4c82dbe636b/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/MessageBodyManager.scala#L54

.. |MessageBodyManager#addByComponentType| replace:: `MessageBodyManager#addByComponentType`
.. _MessageBodyManager#addByComponentType: https://github.com/twitter/finatra/blob/6e09e95b95b20d2599a6210dfa0ce4c82dbe636b/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/MessageBodyManager.scala#L60

.. |MessageBodyModule| replace:: `c.t.finatra.http.modules.MessageBodyModule`
.. _MessageBodyModule:  https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/modules/MessageBodyModule.scala

.. |MessageBodyComponent| replace:: `c.t.finatra.http.marshalling.MessageBodyComponent`
.. _MessageBodyComponent: https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/MessageBodyComponent.scala

.. |MustacheMessageBodyWriter| replace:: `c.t.finatra.mustache.writer.MustacheMessageBodyWriter`
.. _MustacheMessageBodyWriter: https://github.com/twitter/finatra/blob/develop/mustache/src/main/scala/com/twitter/finatra/mustache/writer/MustacheMessageBodyWriter.scala

.. |@Mustache| replace:: ``@Mustache``
.. _@Mustache: https://github.com/twitter/finatra/blob/develop/http-mustache/src/main/java/com/twitter/finatra/http/annotations/Mustache.java
