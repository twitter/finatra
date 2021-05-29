.. _mustache_routing:

Mustache Integration with HTTP Routing
======================================

`Mustache <https://mustache.github.io/>`__ support for HTTP servers is provided by the `finatra/http-mustache <https://github.com/twitter/finatra/blob/develop/mustache/src/main/scala/com/twitter/finatra/http-mustache>`_
library. This integrates Finatra's general `Mustache <https://mustache.github.io/>`__ support with Finatra's
HTTP `MessageBodyComponents <../http/message_body.html>`_.

You must include the |MustacheModule|_ in your server's list of modules in order for the framework
to negotiate rendering of `Mustache <https://mustache.github.io/>`__ templates via `MessageBodyComponents`.

.. code:: scala
    
    import com.google.inject.Module
    import com.twitter.finatra.http.modules.MustacheModule
    import com.twitter.finatra.http.HttpServer

    class MyServer extends HttpServer {
        override val modules: Seq[Module] = Seq(MustacheModule)

        ???
    }

The |MustacheModule|_ includes the general `MustacheFactoryModule` which is explained in the general
`Mustache <https://mustache.github.io/>`__ support documentation `here <../mustache/index.html>`_.

Responses
---------

As a callback return type
~~~~~~~~~~~~~~~~~~~~~~~~~

To generate an HTTP response rendered from a `Mustache <https://mustache.github.io/>`__  template you can simply 
return a |@Mustache|_-annotated object or a `MustacheBodyComponent` as the result of your route callback.

The framework will use `Mustache <https://mustache.github.io/>`__ to render callback return types that 
are annotated with the |@Mustache|_ annotation or are an instance of a `MustacheBodyComponent`. E.g.,

.. code:: scala

    @Mustache("foo")
    case class FooView(
      name: String)

    get("/foo") { request: Request =>
      FooView("abc")
    }


The value of the |@Mustache|_ annotation is assumed by the |MustacheMessageBodyWriter|_ to be the template 
filename without the suffix (which the framework **always assumes** to be `.mustache`).

Thus in the example above, this attempts to locate the `foo.mustache` template and uses the fields of the
`FooView` to populate the template then returns the result as the body of an `200 - OK` response.

Explicitly created response body
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Or you can manually create a response via the |ResponseBuilder|_ which explicitly references a template, e.g.,

.. code:: scala

    import com.twitter.finatra.http.marshalling.response._

    get("/foo") { request: Request =>
      ...
      response.notFound.view(
        template = "notFound.mustache",
        obj = NotFound("abc"))
    }

.. note::
    
    The `#view` methods are implicits added to the `c.t.finatra.http.response.EnrichedResponse` by 
    importing the implicit `RichEnrichedResponse` into scope with
    `import com.twitter.finatra.http.marshalling.response._`.

As part of a response body
~~~~~~~~~~~~~~~~~~~~~~~~~~

Or you can programmatically render a template into a string using 
the `c.t.finatra.mustache.marshalling.MustacheService#createString` method. This is useful for embedding 
the resultant content inside a field in a response.

.. code:: scala
  
    import com.twitter.finatra.mustache.marshalling.MustacheService

    case class TestUserView(
      age: Int,
      name: String,
      friends: Seq[String])

    case class TestCaseClassWithHtml(
      address: String,
      phone: String,
      renderedHtml: String)

    get("/testClassWithHtml") { r: Request =>
      val testUser = TestUserView(
        28,
        "Bob Smith",
        Seq("user1", "user2"))

      TestCaseClassWithHtml(
        address = "123 Main St. Anywhere, CA US 90210",
        phone = "+12221234567",
        renderedHtml = xml.Utility.escape(mustacheService.createString("testHtml.mustache", testUser)))
    }

The `MustacheService` is part of the framework's generic `Mustache <https://mustache.github.io/>`__ support. 
For more information see the `Finatra Mustache <../mustache/index.html>`__ section.

Template Resolving
~~~~~~~~~~~~~~~~~~

To better understand how `Mustache <https://mustache.github.io/>`__ templates are resolved via the 
|@Mustache|_ annotation or from a |MessageBodyComponent|_, please see |MustacheTemplateLookup|_ and the 
corresponding |MustacheTemplateLookupTest|_. 

For more information on referencing files in Finatra, see the `Working with Files <../files/index.html>`__ 
section.

.. |ResponseBuilder| replace:: `c.t.finatra.http.response.ResponseBuilder`
.. _ResponseBuilder: https://github.com/twitter/finatra/blob/develop/http-core/src/main/scala/com/twitter/finatra/http/response/ResponseBuilder.scala

.. |MustacheModule| replace:: `c.t.finatra.http.modules.MustacheModule`
.. _MustacheModule: https://github.com/twitter/finatra/blob/develop/http-mustache/src/main/scala/com/twitter/finatra/http/modules/MustacheModule.scala

.. |@Mustache| replace:: ``@Mustache``
.. _@Mustache: https://github.com/twitter/finatra/blob/develop/http-mustache/src/main/java/com/twitter/finatra/http/annotations/Mustache.java

.. |MessageBodyComponent| replace:: `c.t.finatra.http.marshalling.MessageBodyComponent`
.. _MessageBodyComponent: https://github.com/twitter/finatra/blob/develop/http-core/src/main/scala/com/twitter/finatra/http/marshalling/MessageBodyComponent.scala

.. |MustacheMessageBodyWriter| replace:: `c.t.finatra.mustache.writer.MustacheMessageBodyWriter`
.. _MustacheMessageBodyWriter: https://github.com/twitter/finatra/blob/develop/mustache/src/main/scala/com/twitter/finatra/mustache/writer/MustacheMessageBodyWriter.scala

.. |MustacheTemplateLookup| replace:: `MustacheTemplateLookup`
.. _MustacheTemplateLookup: https://github.com/twitter/finatra/blob/develop/mustache/src/main/scala/com/twitter/finatra/mustache/writer/MustacheTemplateLookup.scala

.. |MustacheTemplateLookupTest| replace:: `MustacheTemplateLookupTest`
.. _MustacheTemplateLookupTest: https://github.com/twitter/finatra/blob/develop/mustache/src/test/scala/com/twitter/finatra/mustache/tests/MustacheTemplateLookupTest.scala
