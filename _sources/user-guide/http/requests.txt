.. _http_requests:

HTTP Requests
=============

Each Finatra `HTTP Controller <controllers.html>`__ route has a callback function which is executed
when the defined route matches an incoming request URI. Callbacks require explicit input types and
Finatra will try to convert the incoming request into the specified input type. Finatra supports three
callback function input types: `a Finagle HTTP Request <#finagle-c-t-finagle-http-request>`__, a
custom `request case class <#custom-request-case-class>`__, or a type parsable by a
`message body reader <#message-body-components>`__. Each are detailed below.

Finagle `c.t.finagle.http.Request`
----------------------------------

This is a `c.t.finagle.http.Request <https://twitter.github.io/finagle/docs/index.html#com.twitter.finagle.http.Request>`__
which contains common HTTP attributes.

Custom "request" case class
---------------------------

Custom "request" case classes can be used for declarative parsing of requests with a content-type of
`application/json` with support for type conversions, default values, and `validations <../json/validations.html>`__.

The case class field names should match the request or header parameter name or use the `@JsonProperty <https://github.com/FasterXML/jackson-annotations#annotations-for-renaming-properties>`__
annotation to specify the JSON field name in the case class (see: `example <https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/jackson/src/test/scala/com/twitter/finatra/json/tests/internal/ExampleCaseClasses.scala#L177>`__
and `test case <https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/jackson/src/test/scala/com/twitter/finatra/json/tests/FinatraObjectMapperTest.scala#L140>`__).

A `PropertyNamingStrategy <https://fasterxml.github.io/jackson-databind/javadoc/2.3.0/com/fasterxml/jackson/databind/PropertyNamingStrategy.html>`__
can be configured to handle common name substitutions (e.g. snake\_case or camelCase). By default,
snake\_case is used (defaults are set in `FinatraJacksonModule <https://github.com/twitter/finatra/tree/master/jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala>`__).

For example, if we have a `POST` endpoint which expects a JSON body with a `Long` `"id"` and a
`String` `"name"`, we could define the following case class and Controller route:

.. code:: scala

  case class HiRequest(id: Long, name: String)

  ...

  post("/hi") { hiRequest: HiRequest =>
    "Hello " + hiRequest.name + " with id " + hiRequest.id
  }

For the request: ``POST /hi``

.. code:: json

  {
    "id": 1,
    "name": "Bob"
  }

The incoming request body will be parsed as JSON using the configured `FinatraObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala>`__
into the `HiRequest` case class. This route would thus return:

   :statuscode 200: `Hello Bob with id 1`


More examples in the `JSON Integration with Routing <../json/routing.html#json-integration-with-routing>`__
section.

For more information on configuring the `FinatraObjectMapper <https://github.com/twitter/finatra/blob/develop/jackson/src/main/scala/com/twitter/finatra/json/FinatraObjectMapper.scala>`__
see the `Jackson Integration <../json/index.html>`__ section.

Required Fields
^^^^^^^^^^^^^^^

**Non-option fields without default values are considered required.**

If a required field is missing, a `CaseClassMappingException` is thrown.

Field Annotations
^^^^^^^^^^^^^^^^^

Field annotations give signal as to how the framework should parse a case class field member from
the `c.t.finagle.http.Request`. Supported annotations:

- `@RouteParam <#routeparam>`__
- `@QueryParam <#queryparam>`__
- `@FormParam <#formparam>`__
- `@Header <#header>`__
- `@Inject <#inject>`__

------------

`@RouteParam <https://github.com/twitter/finatra/blob/develop/jackson/src/main/java/com/twitter/finatra/request/RouteParam.java>`__
"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""

Denotes a field to be parsed from a named parameter in a given route, e.g.,

.. code:: scala

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

Code `example <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/IdAndNameRequest.scala>`__.

.. note::

    Route parameter names are case sensitive.

------------

`@QueryParam <https://github.com/twitter/finatra/blob/develop/jackson/src/main/java/com/twitter/finatra/request/QueryParam.java>`__
"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""

Read a value from the request query string by a parameter named for the case class field or by the
``@QueryParam`` annotation value.

For example, suppose you want to parse a `GET` request with three query params: `max`, `startDate`,
and `verbose`, e.g.,

``GET /users?max=10&start_date=2014-05-30TZ&verbose=true``

This can be modeled with the following custom "request" case class which also applies
`validations <../json/validations.html>`__:

.. code:: scala

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

.. code:: scala

  case class QueryParamRequest(
    @QueryParam foo: String,
    @QueryParam("skip") isSkipped: Boolean)

Using this case class in a route callback for a request:

``GET /?foo=bar&skip=false``

would parse the string "bar" into the value of `QueryParamRequest#foo` and parse the string "false"
as a Boolean into the `QueryParamRequest#isSkipped` field.

Code `example <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/RequestWithQueryParamSeqString.scala>`__.

You can parse comma-separated lists in your query parameters by setting `commaSeparatedList` to `true` in the QueryParam annotation, e.g.

.. code:: scala

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

`@FormParam <https://github.com/twitter/finatra/blob/develop/jackson/src/main/java/com/twitter/finatra/request/FormParam.java>`__
"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""

Read a value from a form field with the case class field's name or as the value specified in the
``@FormParam`` annotation from the request body.

Code `example <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/FormPostRequest.scala>`__.

.. note::

    Form parameter names are case sensitive.

------------

`@Header <https://github.com/twitter/finatra/blob/develop/jackson/src/main/java/com/twitter/finatra/request/Header.java>`__
"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""

Read a header value specified by the case class field name or by the ``@Header`` annotation value.
You can use a Scala `"back-quote" literal <https://www.scala-lang.org/files/archive/spec/2.11/01-lexical-syntax.html>`__
for the field name when special characters are involved.

.. code:: scala

  @Header `user-agent`: String

or specify the header name as a parameter to the ``@Header`` annotation, e.g.,

.. code:: scala

  @Header("user-agent") agent: String

Code `example <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/CreateUserRequest.scala>`__.

.. important::

    Route, query, and form params are all stored in the "params" field of the incoming Finagle http
    request. As such, you should ensure that ``@RouteParam`` names do not collide with ``@QueryParam``
    names. Otherwise, an ``@QueryParam`` could end up parsing an ``@RouteParam`` or ``@FormParam``
    field.

    Also note that headers are accessed **case-insensitively**. Thus, the annotated fields:

    .. code:: scala

        @Header("Accept-Charset") acceptCharset: String
        @Header("accept-charset") acceptCharset: String
        @Header("aCcEpT-cHaRsEt") acceptCharset: String
        @Header `accept-charset`: String

    would all retrieve **the same value** from the request headers map.

------------

@Inject
"""""""

Can be used to inject any `Guice <https://github.com/google/guice>`__ managed class into your case
class.


.. code:: scala

    case class InjectedFieldCaseClass(
      @Inject defaultOptString: Option[String],
      @Inject foo Foo)

Note, however, it is not necessary to "inject" the underlying Finagle `http` Request into your
case class. To access the underlying Finagle HTTP Request in your custom case class, simply
include a field of type `c.t.finagle.http.Request` and the framework will properly inject the
incoming request into your custom case class, for example:

.. code:: scala

    import com.twitter.finagle.http.Request

    case class CaseClassWithRequestField(
     @Header("user-agent") agent: String,
     @QueryParam verbose: Boolean = false,
     request: Request)

Code `example <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/RequestWithInjections.scala>`__.

------------

.. note::

    HTTP requests with a content-type of `application/json` sent to routes with a custom request
    case class callback input type will **always trigger** the parsing of the request body as
    well-formed JSON in attempt to convert the JSON into the request case class.

    This behavior can be disabled by annotating the case class with ``@JsonIgnoreBody`` leaving the
    raw request body accessible by simply adding a member of type `c.t.finagle.http.Request` as
    mentioned above.

For more specifics on how JSON parsing integrates with routing see the
`JSON Integration with Routing <../json/routing.html>`__ in the `JSON <../json/index.html>`__ documentation.

Message Body Components
-----------------------

Finatra also provides a way to specify how to parse the request into a callback function input via a
`Message Body Reader <message_body.html#message-body-readers>`__. Similar to `custom request case
classes <#custom-request-case-class>`__ (which are implemented with a framework
`default message body reader <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/DefaultMessageBodyReaderImpl.scala>`__),
these objects allow for customized parsing the full incoming `c.t.finagle.http.Request` into a
specific type. For more information, see the `Message Body Readers <message_body.html#message-body-readers>`__
section.

Request Forwarding
------------------

You can forward a request to another controller. This is similar to other frameworks where
forwarding will re-use the same request as opposed to issuing a redirect which will force a client
to issue a new request.

To forward, you need to include a `c.t.finatra.http.request.HttpForward` instance in your controller,
e.g.,

.. code:: scala

    import com.twitter.finagle.http.Request
    import com.twitter.finatra.http.Controller

    class MyController @Inject()(
      forward: HttpForward)
      extends Controller {


Then, to use in your route:

.. code:: scala

    get("/foo") { request: Request =>
      forward(request, "/bar")
    }

Forwarded requests will bypass the server defined filter chain (as the requests have already passed
through the filter chain) but will still pass through controller defined filters.

For example, if a route is defined:

.. code:: scala

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

    .. code:: scala

        override def configureHttp(router: HttpRouter) {
          router
            .withMaxRequestForwardingDepth(10)
            .add[MyController]
        }

Multipart Requests
------------------

Finatra has support for multi-part requests. Here's an example of a multi-part `POST` controller
route definition that simply returns all of the keys in the multi-part request:

.. code:: scala

    post("/multipartParamsEcho") { request: Request =>
      RequestUtils.multiParams(request).keys
    }


An example of testing this endpoint:

.. code:: scala

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

.. code:: scala

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

.. code:: scala

    import com.twitter.finatra.http.Controller

    class MyController @Inject()(
      jsonPatchOperator: JsonPatchOperator
    ) extends Controller {
      ???
    }

After the target data has been converted to a JsonNode, just call `JsonPatchUtility.operate` to apply
JSON Patch operations to the target.

For example:

.. code:: scala

    patch("/jsonPatch") { jsonPatch: JsonPatch =>
      val testCase = ExampleCaseClass("world")
      val originalJson = jsonPatchOperator.toJsonNode[ExampleCaseClass](testCase)
      JsonPatchUtility.operate(jsonPatch.patches, jsonPatchOperator, originalJson)
    }

An example of testing this endpoint:

.. code:: scala

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

-  `c.t.finatra.http.request.RequestUtils <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/request/RequestUtils.scala>`__
-  `c.t.finatra.http.fileupload.MultipartItem <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/fileupload/MultipartItem.scala>`__
-  `DoEverythingController <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/controllers/DoEverythingController.scala>`__
-  `DoEverythingServerFeatureTest <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/test/DoEverythingServerFeatureTest.scala>`__
-  `MultiParamsTest <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/request/MultiParamsTest.scala>`__
