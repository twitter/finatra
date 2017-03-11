.. _http_requests:

HTTP Requests
=============

Each route has a callback which is executed when the route matches a request. Callbacks require explicit input types and Finatra will then try to convert the incoming request into the specified input type. Finatra supports two request types: a Finagle `http` Request or a custom `case class` request object.

Finagle `c.t.finagle.http.Request`
----------------------------------

This is a `c.t.finagle.http.Request <https://twitter.github.io/finagle/docs/index.html#com.twitter.finagle.http.Request>`__ which contains common HTTP attributes.

Custom case class request object
--------------------------------

Custom request case classes are used for requests with a content-type of `application/json` and allow declarative request parsing with support for type conversions, default values, and validations.

For example, suppose you want to parse a `GET` request with three query params: `max`, `startDate`, and `verbose`,

.. code:: text

    http://foo.com/users?max=10&start_date=2014-05-30TZ&verbose=true


This can be modeled with the following case class:

.. code:: scala

    case class UsersRequest(
      @Max(100) @QueryParam max: Int,
      @PastDate @QueryParam startDate: Option[DateTime],
      @QueryParam verbose: Boolean = false)


The custom `UsersRequest` case class can then be used as the route callback's input type:

.. code:: scala

    get("/users") { request: UsersRequest =>
      request
    }

The case class field names should match the request parameters or use the `@JsonProperty <https://github.com/FasterXML/jackson-annotations#annotations-for-renaming-properties>`__ annotation to specify the JSON field name in the case class (see: `example <https://github.com/twitter/finatra/blob/develop/jackson/src/test/scala/com/twitter/finatra/tests/json/internal/ExampleCaseClasses.scala#L141>`__).

A `PropertyNamingStrategy <https://fasterxml.github.io/jackson-databind/javadoc/2.3.0/com/fasterxml/jackson/databind/PropertyNamingStrategy.html>`__ can be configured to handle common name substitutions (e.g. snake\_case or camelCase). By default, snake\_case is
used (defaults are set in `FinatraJacksonModule <https://github.com/twitter/finatra/tree/master/jackson/src/main/scala/com/twitter/finatra/json/modules/FinatraJacksonModule.scala>`__).

Required Fields
^^^^^^^^^^^^^^^

**Non-option fields without default values are considered required.**

If a required field is missing, a `CaseClassMappingException` is thrown.

Field Annotations
^^^^^^^^^^^^^^^^^

The following field annotations specify how or where to parse a case class field out of the request:

- `@RouteParam <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/IdAndNameRequest.scala>`__  - denotes a field to be parsed from a named parameter in a given route, e.g., for a route defined as: `get("/foo/:id")`

  .. code:: scala

      case class FromRouteRequest(
        @RouteParam id: String)

  would take a request with a URI of `/foo/1234` and set `1234` as the value of the `id` field in the `FromRouteRequest` case class.

- `@QueryParam <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/RequestWithQueryParamSeqString.scala>`__ - reads the value from the query string by a parameter named for the field. Make sure that ``@RouteParam`` names do not collide with
  ``@QueryParam`` names. Otherwise, an ``@QueryParam`` could end up parsing an ``@RouteParam``. E.g.,

  .. code:: scala

      case class QueryParamRequest(
        @QueryParam foo: String)

  would set the value of the `foo` field in the `QueryParamRequest` to the result of `request.param("foo")`.

- `@FormParam <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/FormPostRequest.scala>`__ - read the value from a form field with the field's name from the request body.
- `@Header <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/CreateUserRequest.scala>`__ - read the value from a header field with the field's name. You can use a Scala `"back-quote" literal <http://www.scala-lang.org/files/archive/spec/2.11/01-lexical-syntax.html>`__ for the field name when special characters are involved. E.g.

  .. code:: scala

      @Header `user-agent`: String

- `@Inject <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/domain/RequestWithInjections.scala>`__ - can be used to inject any Guice managed class into your case class. However, it is not necessary for "injecting" the underlying Finagle `http` Request into your case class. To access the underlying Finagle `http` Request in your custom case class, simply include a field of type `c.t.finagle.http.Request`, for example:

  .. code:: scala

      case class CaseClassWithRequestField(
       @Header `user-agent`: String,
       @QueryParam verbose: Boolean = false,
       request: Request)

**Note:** HTTP requests with a content-type of `application/json` sent to routes with a custom request case class callback input type will **always trigger** the parsing of the request body as well-formed JSON in attempt to convert the JSON into the request case class.

This behavior can be disabled by annotating the case class with ``@JsonIgnoreBody`` leaving the raw request body accessible by simply adding a member of type `c.t.finagle.http.Request` as mentioned above.

For more specifics on how JSON parsing integrates with routing see: `Integration with Routing <../json/routing.html>`__ in the `JSON <../json/index.html>`__ documentation.

Request Forwarding
------------------

You can forward a request to another controller. This is similar to other frameworks where forwarding will re-use the same request as opposed to issuing a redirect which will force a client to issue a new request.

To forward, you need to include a `c.t.finatra.http.request.HttpForward` instance in your controller,
e.g.,

.. code:: scala

    class MyController @Inject()(
      forward: HttpForward)
      extends Controller {


Then, to use in your route:

.. code:: scala

    get("/foo") { request: Request =>
      forward(request, "/bar")
    }


Forwarded requests will bypass the server defined filter chain (as the requests have already passed through the filter chain) but will still pass through controller defined filters.

For example, if a route is defined:

.. code:: scala

    filter[MyAwesomeFilter].get("/bar") { request: Request =>
      "Hello, world."
    }


When another controller forwards to this route, `MyAwesomeFilter` will be executed on the forwarded request.

Multipart Requests
------------------

Finatra has support for multi-part requests. Here's an example of a multi-part `POST` controller route definition that simply returns all of the keys in the multi-part request:

.. code:: scala

    post("/multipartParamsEcho") { request: Request =>
      RequestUtils.multiParams(request).keys
    }


An example of testing this endpoint:

.. code:: scala

    def deserializeRequest(name: String) = {
      val requestBytes = IOUtils.toByteArray(getClass.getResourceAsStream(name))
      Request.decodeBytes(requestBytes)
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

To handle JSON Patch requests, you will first need to register the `JsonPatchMessageBodyReader` and the `JsonPatchExceptionMapper` in the server. The `JsonPatchMessageBodyReader` is for parsing JSON Patch requests as type `c.t.finatra.http.jsonpatch.JsonPatch`,
and `JsonPatchExceptionMapper` can convert JsonPatchExceptions to HTTP responses.

See `Add an ExceptionMapper <exceptions.html>`__ for more information on exception mappers.

.. code:: scala

    class ExampleServer extends HttpServer {

     override def configureHttp(router: HttpRouter): Unit = {
       router
         .register[JsonPatchMessageBodyReader]
         .exceptionMapper[JsonPatchExceptionMapper]
         .add[ExampleController]
     }
    }


Next, you should include a `c.t.finatra.http.jsonpatch.JsonPatchOperator` instance in your controller, which provides `JsonPatchOperator#toJsonNode` conversions and support for all JSON Patch operations.

.. code:: scala

    class MyController @Inject()(
      jsonPatchOperator: JsonPatchOperator
    ) extends Controller {
      ...

    }

After the target data has been converted to a JsonNode, just call `JsonPatchUtility.operate` to apply JSON Patch operations to the target.

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
-  `c.t.finagle.http.Request#decodeBytes <https://github.com/twitter/finagle/blob/develop/finagle-http/src/main/scala/com/twitter/finagle/http/Request.scala#L192>`__
-  `DoEverythingController <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/main/controllers/DoEverythingController.scala#L568>`__
-  `DoEverythingServerFeatureTest <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/test/DoEverythingServerFeatureTest.scala#L332>`__
-  `MultiParamsTest <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/request/MultiParamsTest.scala>`__