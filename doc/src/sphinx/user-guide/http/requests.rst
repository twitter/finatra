.. _http_requests:

HTTP Requests
=============

Each Finatra `HTTP Controller <controllers.html>`__ route must specify a callback function which is executed
when the defined route matches an incoming request `HTTP method <https://github.com/twitter/finagle/blob/a7867d088ea15620c375bfb5649071c16bc6360a/finagle-base-http/src/main/scala/com/twitter/finagle/http/Method.scala#L24>`__ and URI: 

.. code-block:: scala

    method("/URI")(callback: T => Any)


The callback function must declare an explicit input type which the framework will attempt to marshal the incoming 
Finagle HTTP Request into via the `c.t.finatra.http.intenal.routing.CallbackConverter <https://github.com/twitter/finatra/blob/develop/http-server/src/main/scala/com/twitter/finatra/http/internal/routing/CallbackConverter.scala>`__. 

Finatra supports three callback function input types: 

- `a Finagle HTTP Request <#finagle-c-t-finagle-http-request>`__
- a `custom "request" case class <#custom-request-case-class>`__
- or a type `T` parsable by a `MessageBodyReader\[T\] <#message-body-components>`__

Each are detailed below.

Finagle `c.t.finagle.http.Request`
----------------------------------

This is a `c.t.finagle.http.Request <https://twitter.github.io/finagle/docs/com/twitter/finagle/http/Request.html>`__
which contains common HTTP attributes.

.. code-block:: scala

  import com.twitter.finagle.http.Request

  ...

  get("/foo") { request: Request =>
    ???
  }

Custom "request" case class
---------------------------

Custom "request" case classes can be used for declarative parsing of HTTP requests with deep support for JSON body parsing,
type conversions, default values, and `validations <../json/validations.html>`__.

Note, that the case class represents a *model* of your HTTP request but is not an actual Finagle HTTP Request. Also note 
that when using a custom "request" case class, you can still `access and use the actual underlying Finagle HTTP request <#accessing-the-underlying-finagle-http-request>`__.

The case class field names should match the request or header parameter name or should use the `@JsonProperty <https://github.com/FasterXML/jackson-annotations#annotations-for-renaming-properties>`__
annotation to specify the expected JSON field name in the request body (see: `example <https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/jackson/src/test/scala/com/twitter/finatra/json/tests/internal/ExampleCaseClasses.scala#L177>`__
and `test case <https://github.com/twitter/finatra/blob/01fdd9cfb3c877fe226085bf411f42ae08420e5d/jackson/src/test/scala/com/twitter/finatra/jackson/tests/AbstractScalaObjectMapperTest.scala#L283>`__).

A `PropertyNamingStrategy <https://fasterxml.github.io/jackson-databind/javadoc/2.3.0/com/fasterxml/jackson/databind/PropertyNamingStrategy.html>`__
can be configured to handle common name substitutions (e.g. `snake\_case` or `camelCase`). By default,
`snake\_case` is used (defaults are set by the `ScalaObjectMapper` provided by the `ScalaObjectMapperModule <https://github.com/twitter/finatra/tree/master/jackson/src/main/scala/com/twitter/finatra/jackson/modules/ScalaObjectMapperModule.scala>`__).

For example, if we have a `POST` endpoint which expects a JSON body with a `Long` `"id"` and a
`String` `"name"`, we could define the following case class and Controller route:

.. code-block:: scala

  case class HiRequest(id: Long, name: String)

  ...

  post("/hi") { hiRequest: HiRequest =>
    "Hello " + hiRequest.name + " with id " + hiRequest.id
  }

For the request: ``POST /hi``

.. code-block:: json

  {
    "id": 1,
    "name": "Bob"
  }

The incoming request body will be parsed as JSON using the configured |ScalaObjectMapper|_
into the `HiRequest` case class. This route would thus return:

   :statuscode 200: `Hello Bob with id 1`


More examples in the `JSON Integration with Routing <../json/routing.html#json-integration-with-routing>`__
section.

For more information on configuring the |ScalaObjectMapper|_ see the
`Jackson Integration <../json/index.html>`__ section.

Required Fields
~~~~~~~~~~~~~~~

**Non-option fields without default values are considered required.**

If a required field is missing, a `CaseClassMappingException` is thrown.

Field Annotations
~~~~~~~~~~~~~~~~~

Field annotations are for parsing case class fields that do not come from the incoming
request body and tell the framework how to parse the case class member from the incoming
the `c.t.finagle.http.Request`. These annotations are implemented via a Finatra HTTP integration 
with Jackson `InjectableValues <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/InjectableValues.java>`_
implemented in the `MessageInjectableValues <https://github.com/twitter/finatra/blob/develop/http-core/src/main/scala/com/twitter/finatra/http/marshalling/MessageInjectableValues.scala>`_ class.

Supported annotations:

- `@RouteParam <#routeparam>`__
- `@QueryParam <#queryparam>`__
- `@FormParam <#formparam>`__
- `@Header <#header>`__
- `@javax.inject.Inject <#javax-inject-inject>`__ (allows for injection of a instance from the object graph).

.. caution::

    The `MessageInjectableValues <https://github.com/twitter/finatra/blob/develop/http-core/src/main/scala/com/twitter/finatra/http/marshalling/MessageInjectableValues.scala>`_
    is only configured via the framework's `DefaultMessageBodyReader <./message_body.html#id2>`_
    `DefaultMessageBodyReaderImpl <https://github.com/twitter/finatra/blob/develop/http-core/src/main/scala/com/twitter/finatra/http/marshalling/DefaultMessageBodyReaderImpl.scala>`_
    which is bound in the `MessageBodyModule <./message_body.html#id5>`_.

    Thus, attempting to use the HTTP-specific field annotations with a |ScalaObjectMapper|_
    instance **will not work**.

    That is, trying to deserialize JSON into a case class that has fields annotated with
    `@QueryParam`, `@RouteParam`, `@FormParam` or `@Header` with an instance of the
    |ScalaObjectMapper|_ will not properly inject those fields from a given HTTP message.

    This injection will only happen properly through usage of the Finatra framework's
    `DefaultMessageBodyReader#parse <./message_body.html#id2>`_.

------------

`@RouteParam <https://github.com/twitter/finatra/blob/develop/http-annotations/src/main/java/com/twitter/finatra/http/annotations/RouteParam.java>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Denotes a field to be parsed from a named parameter in a given route, e.g.,

.. code-block:: scala

  case class FromRouteRequest(
    @RouteParam("entity") resource: String,
    @RouteParam id: String)


  get("/foo/:entity/:id") { request: FromRouteRequest =>
    s"The resource is ${request.resource} and the id = ${request.id}"
  }

Given a request: ``GET /foo/users/1234``

Using `FromRouteRequest` as an input to the route callback would parse the string "users" into the
value of `FromRouteRequest#resource` and the string "1234" into the value of `FromRouteRequest#id`.

Thus, this route would respond:

   :statuscode 200: `The resource is users and the id = 1234`

Code `example <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/IdAndNameRequest.scala>`__.

.. note::

    Route parameter names are case sensitive.

------------

`@QueryParam <https://github.com/twitter/finatra/blob/develop/http-annotations/src/main/java/com/twitter/finatra/http/annotations/QueryParam.java>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Read a value from the request query string by a parameter named for the case class field or by the
``@QueryParam`` annotation value.

For example, suppose you want to parse a `GET` request with three query params: `max`, `startDate`,
and `verbose`, e.g.,

``GET /users?max=10&start_date=2014-05-30TZ&verbose=true``

This can be modeled with the following custom "request" case class which also applies
`validations <../json/validations.html>`__:

.. code-block:: scala

  case class UsersRequest(
    @Max(100) @QueryParam max: Int,
    @PastDate @QueryParam startDate: Option[DateTime],
    @QueryParam verbose: Boolean = false)

  get("/users") { request: UsersRequest =>
    ???
  }

The `max` value will be parsed into an `Int` and `validated to be less than or equal to
100 <https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/jackson/src/main/scala/com/twitter/finatra/json/internal/caseclass/validation/validators/MaxValidator.scala#L49>`__.
The `startDate` will be parsed into an `Option[DateTime]` (meaning it could be omitted without error
from the query string) and if present will be validated to be a date in the past. Lastly, the
`verbose` parameter will be parsed into a `Boolean` type.

You can also set the parameter name as a value in the ``@QueryParam`` annotation, e.g.

.. code-block:: scala

  case class QueryParamRequest(
    @QueryParam foo: String,
    @QueryParam("skip") isSkipped: Boolean)

Using this case class in a route callback for a request:

``GET /?foo=bar&skip=false``

would parse the string "bar" into the value of `QueryParamRequest#foo` and parse the string "false"
as a Boolean into the `QueryParamRequest#isSkipped` field.

Code `example <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/RequestWithQueryParamSeqString.scala>`__.

You can parse comma-separated lists in your query parameters by setting `commaSeparatedList` to `true` in the QueryParam annotation, e.g.

.. code-block:: scala

  case class ManyUsersRequest(
    @QueryParam(commaSeparatedList=true) ids: Seq[Long])

Using this class in a route callback for a request:

``GET /?ids=1,2,3``

would split the string "1,2,3" into "1", "2", and "3", and then parse each into a `Long`.

Note that turning this on will disallow repeating the 'ids' parameter, ie.

``GET /?ids=1&ids=2,3``

will return a Bad Request with an appropriate error message when `commaSeparatedList` is `true`.

.. note::

    Query parameter names are case sensitive.

------------

`@FormParam <https://github.com/twitter/finatra/blob/develop/http-annotations/src/main/java/com/twitter/finatra/http/annotations/FormParam.java>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Read a value from a form field with the case class field's name or as the value specified in the
``@FormParam`` annotation from the request body.

Code `example <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/FormPostRequest.scala>`__.

.. note::

    Form parameter names are case sensitive.

------------

`@Header <https://github.com/twitter/finatra/blob/develop/http-annotations/src/main/java/com/twitter/finatra/http/annotations/Header.java>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Read a header value specified by the case class field name or by the ``@Header`` annotation value.
You can use a Scala `"back-quote" literal <https://www.scala-lang.org/files/archive/spec/2.11/01-lexical-syntax.html>`__
for the field name when special characters are involved.

.. code-block:: scala

  @Header `user-agent`: String

or specify the header name as a parameter to the ``@Header`` annotation, e.g.,

.. code-block:: scala

  @Header("user-agent") agent: String

Code `example <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/CreateUserRequest.scala>`__.

.. important::

    Route, query, and form params are all stored in the "params" field of the incoming Finagle http
    request. As such, you should ensure that ``@RouteParam`` names do not collide with ``@QueryParam``
    names. Otherwise, an ``@QueryParam`` could end up parsing an ``@RouteParam`` or ``@FormParam``
    field.

    Also note that headers are accessed **case-insensitively**. Thus, the annotated fields:

    .. code-block:: scala

        @Header("Accept-Charset") acceptCharset: String
        @Header("accept-charset") acceptCharset: String
        @Header("aCcEpT-cHaRsEt") acceptCharset: String
        @Header `accept-charset`: String

    would all retrieve **the same value** from the request headers map.

------------

@javax.inject.Inject
^^^^^^^^^^^^^^^^^^^^

Can be used to inject any `Guice <https://github.com/google/guice>`__ managed class into your case
class.

.. code-block:: scala

    import javax.inject.Inject

    case class InjectedFieldCaseClass(
      @Inject bar: Bar,
      @Inject foo Foo)

Note that we use `@javax.inject.Inject` here but as mentioned in the 
`Jackson InjectableValues Support <../json/index.html#jackson-injectablevalues-support>`__ documention, 
`com.google.inject.Inject` or `com.fasterxml.jackson.annotation.JacksonInject` are also supported
annotations for injecting fields via Jackson `InjectableValues <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/InjectableValues.java>`_.

Code `example <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/RequestWithInjections.scala>`__.

Accessing the Underlying Finagle HTTP Request
+++++++++++++++++++++++++++++++++++++++++++++

It is not necessary to `@Inject` the underlying Finagle HTTP Request into your
case class. The framework `MessageInjectableValues <https://github.com/twitter/finatra/blob/develop/http-core/src/main/scala/com/twitter/finatra/http/marshalling/MessageInjectableValues.scala>`_ specifically knows how to "inject"
the current Finagle HTTP Request into a case class during request parsing. 

Thus, to access the underlying Finagle HTTP Request in your custom "request" case class, include 
a field of type `c.t.finagle.http.Request` and the framework will properly inject the incoming 
Finagle HTTP Request into your custom case class, for example:

.. code-block:: scala

    import com.twitter.finagle.http.Request
    import com.twitter.finatra.http.annotations.Header
    import com.twitter.finatra.http.annotations.QueryParam

    case class CaseClassWithRequestField(
     @Header("user-agent") agent: String,
     @QueryParam verbose: Boolean = false,
     request: Request)

.. note::

  The `MessageInjectableValues <https://github.com/twitter/finatra/blob/develop/http-core/src/main/scala/com/twitter/finatra/http/marshalling/MessageInjectableValues.scala>`_ is doing the work to provide the Finagle HTTP Request
  as context in a Jackson `InjectableValues <https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/InjectableValues.java>`_ implementation that can be used for object mapping the incoming Finagle HTTP Request.

  Because the current Finagle HTTP Request is necessary, the `MessageInjectableValues <https://github.com/twitter/finatra/blob/develop/http-core/src/main/scala/com/twitter/finatra/http/marshalling/MessageInjectableValues.scala>`_ 
  is only available via the `DefaultMessageBodyReader#parse <./message_body.html#id2>`_ which expects a Finagle HTTP `c.t.finagle.http.Message <https://github.com/twitter/finagle/blob/develop/finagle-base-http/src/main/scala/com/twitter/finagle/http/Message.scala>`__ (and not as part of the ScalaObjectMapper -- which knows nothing about HTTP).


Do Not Use Multiple Field Annotations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. warning::

    It is an error to specify multiple field annotations on a single case class field, and it is also an
    error to use a field annotation in conjunction with **any** `JacksonAnnotation <https://github.com/FasterXML/jackson-annotations/blob/a991c43a74e4230eb643e380870b503997674c2d/src/main/java/com/fasterxml/jackson/annotation/JacksonAnnotation.java#L9>`_.

    Both of these cases will result in error during deserialization of JSON into the case class.

Disable Automatic Request Body Parsing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When a Controller route callback is specified to use a custom "request" case class as the callback
input type, any incoming request with a `Content-Type` of `application/json` with trigger the framework
to parse the incoming Finagle HTTP Request body as JSON in order to convert into the custom "request" 
case class.

.. code-block:: scala

    import com.twitter.finatra.http.annotations.Header
    import com.twitter.finatra.http.annotations.RouteParam

    case class MyGreatCaseClass(
     @Header("user-agent") agent: String,
     @RouteParam id: String)

    ...

    post("/foo/:id") { request: MyGreatCaseClass =>
      ???
    }

In the above example if the HTTP request is sent with a `Content-Type` header of `application/json`, 
the framework will try to parse the incoming request body as JSON. There is no need to have the
framework attempt to read the request body as JSON to map it to the `MyGreatCaseClass` case class 
since none of the custom "request" case class fields come from the request body.

In cases like this, the automatic request body parsing behavior can be disabled by annotating the 
custom "request" case class with ``@JsonIgnoreBody``. 

.. code-block:: scala
   :emphasize-lines: 5

    import com.twitter.finatra.http.annotations.Header
    import com.twitter.finatra.http.annotations.JsonIgnoreBody
    import com.twitter.finatra.http.annotations.RouteParam

    @JsonIgnoreBody
    case class MyGreatCaseClass(
     @Header("user-agent") agent: String,
     @RouteParam id: String)

If necessary, you can access the request body (or any other attribute) directly from the 
Finagle HTTP Request by adding a member of type `c.t.finagle.http.Request` as mentioned above in the 
`Accessing the Underlying Finagle HTTP Request <#accessing-the-underlying-finagle-http-request>`__ section.

Message Body Components
-----------------------

Finatra also provides a more general way to specify how to marhsal the incoming Finagle HTTP Request into 
a callback function input type `T` using a `MessageBodyReader\[T\] <message_body.html#message-body-readers>`__. 

Similar to `custom "request" case classes <#custom-request-case-class>`__ -- which are supported via the framework
`DefaultMessageBodyReader <https://github.com/twitter/finatra/blob/develop/http-core/src/main/scala/com/twitter/finatra/http/marshalling/DefaultMessageBodyReaderImpl.scala>`__ implementation, message body readers allow for customized parsing of 
the full incoming `c.t.finagle.http.Request` into a specific type, `T`. 

For more information, see the `MessageBodyReader\[T\] <./message_body.html#messagebodyreader-t>`__
section of the `Message Body Components <./message_body.html>`__ documentation.

Request Forwarding
------------------

You can forward a request to another controller. This is similar to other frameworks where
forwarding will re-use the same request as opposed to issuing a redirect which will force a client
to issue a new request.

To forward, you need to include a `c.t.finatra.http.request.HttpForward` instance in your controller,
e.g.,

.. code-block:: scala

    import com.twitter.finagle.http.Request
    import com.twitter.finatra.http.Controller

    class MyController @Inject()(
      forward: HttpForward)
      extends Controller {


Then, to use in your route:

.. code-block:: scala

    get("/foo") { request: Request =>
      forward(request, "/bar")
    }

Forwarded requests will bypass the server defined filter chain (as the requests have already passed
through the filter chain) but will still pass through controller defined filters.

For example, if a route is defined:

.. code-block:: scala

    filter[MyAwesomeFilter].get("/bar") { request: Request =>
      "Hello, world."
    }

When another controller forwards to this route, `MyAwesomeFilter` will be executed on the forwarded
request.

.. important::
    By default Finatra sets a maximum forward depth of 5. This value is configurable by setting the
    `HttpRouter#withMaxRequestForwardingDepth`. This helps prevent a given request from being
    forwarded in an infinite loop.

    In the example below, the server has been setup to allow a request to forward a maximum of 10 times.

    .. code-block:: scala

        override def configureHttp(router: HttpRouter) {
          router
            .withMaxRequestForwardingDepth(10)
            .add[MyController]
        }

Multipart Requests
------------------

Finatra has support for multi-part requests. Here's an example of a multi-part `POST` controller
route definition that simply returns all of the keys in the multi-part request:

.. code-block:: scala

    post("/multipartParamsEcho") { request: Request =>
      RequestUtils.multiParams(request).keys
    }


An example of testing this endpoint:

.. code-block:: scala

    def deserializeRequest(name: String) = {
      val requestBytes = IOUtils.toByteArray(getClass.getResourceAsStream(name))
      HttpCodec.decodeBytesToRequest(requestBytes)
    }

    "post multipart" in {
      val request = deserializeRequest("/multipart/request-POST-android.bytes")
      request.uri = "/multipartParamsEcho"

      server.httpRequest(
        request = request,
        suppress = true,
        andExpect = Ok,
        withJsonBody = """["banner"]""")
    }


JSON Patch Requests
-------------------

Finatra has support for JSON Patch requests, see `JSON Patch definition <https://tools.ietf.org/html/rfc6902>`__.

To handle JSON Patch requests, you will first need to register the `JsonPatchMessageBodyReader` and
the `JsonPatchExceptionMapper` in the server. The `JsonPatchMessageBodyReader` is for parsing JSON
Patch requests as type `c.t.finatra.http.jsonpatch.JsonPatch`, and `JsonPatchExceptionMapper` can
convert JsonPatchExceptions to HTTP responses.

See `Add an ExceptionMapper <exceptions.html>`__ for more information on exception mappers.

.. code-block:: scala

    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.routing.HttpRouter

    class ExampleServer extends HttpServer {

     override def configureHttp(router: HttpRouter): Unit = {
       router
         .register[JsonPatchMessageBodyReader]
         .exceptionMapper[JsonPatchExceptionMapper]
         .add[ExampleController]
     }
    }


Next, you should include a `c.t.finatra.http.jsonpatch.JsonPatchOperator` instance in your controller,
which provides `JsonPatchOperator#toJsonNode` conversions and support for all JSON Patch operations.

.. code-block:: scala

    import com.twitter.finatra.http.Controller

    class MyController @Inject()(
      jsonPatchOperator: JsonPatchOperator
    ) extends Controller {
      ???
    }

After the target data has been converted to a JsonNode, just call `JsonPatchUtility.operate` to apply
JSON Patch operations to the target.

For example:

.. code-block:: scala

    patch("/jsonPatch") { jsonPatch: JsonPatch =>
      val testCase = ExampleCaseClass("world")
      val originalJson = jsonPatchOperator.toJsonNode[ExampleCaseClass](testCase)
      JsonPatchUtility.operate(jsonPatch.patches, jsonPatchOperator, originalJson)
    }

An example of testing this endpoint:

.. code-block:: scala

    "JsonPatch" in {
      val request = RequestBuilder.patch("/jsonPatch")
        .body(
          """[
            |{"op":"add","path":"/fruit","value":"orange"},
            |{"op":"remove","path":"/hello"},
            |{"op":"copy","from":"/fruit","path":"/veggie"},
            |{"op":"replace","path":"/veggie","value":"bean"},
            |{"op":"move","from":"/fruit","path":"/food"},
            |{"op":"test","path":"/food","value":"orange"}
            |]""".stripMargin,
          contentType = Message.ContentTypeJsonPatch)

      server.httpRequestJson[JsonNode](
        request = request,
        andExpect = Ok,
        withJsonBody = """{"food":"orange","veggie":"bean"}""")
    }


For more information and examples, see:

-  `c.t.finatra.http.request.RequestUtils <https://github.com/twitter/finatra/blob/develop/http-core/src/main/scala/com/twitter/finatra/http/request/RequestUtils.scala>`__
-  `c.t.finatra.http.fileupload.MultipartItem <https://github.com/twitter/finatra/blob/develop/http-core/src/main/scala/com/twitter/finatra/http/fileupload/MultipartItem.scala>`__
-  `DoEverythingController <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/controllers/DoEverythingController.scala>`__
-  `DoEverythingServerFeatureTest <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/test/DoEverythingServerFeatureTest.scala>`__
-  `MultiParamsTest <https://github.com/twitter/finatra/blob/develop/http-server/src/test/scala/com/twitter/finatra/http/tests/request/MultiParamsTest.scala>`__

.. |ScalaObjectMapper| replace:: `ScalaObjectMapper`
.. _ScalaObjectMapper: https://github.com/twitter/util/blob/develop/util-jackson/src/main/scala/com/twitter/util/jackson/ScalaObjectMapper.scala

