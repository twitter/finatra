.. _http_responses:

HTTP Responses
==============

JSON responses
--------------

The simplest way to return a JSON response is to return a case class in your route callback. The default framework behavior is to render the case class as a JSON response E.g.,

.. code:: scala

    case class ExampleCaseClass(
      id: String,
      description: String,
      longValue: Long,
      boolValue: Boolean)

    get("/foo") { request: Request => 
      ExampleCaseClass(
        id = "123",
        description = "This is a JSON response body",
        longValue = 1L,
        boolValue = true)
    }


will produce a response:

::

    [Status]  Status(200)
    [Header]  Content-Type -> application/json; charset=utf-8
    [Header]  Server -> Finatra
    [Header]  Date -> Wed, 17 Aug 2015 21:54:25 GMT
    [Header]  Content-Length -> 90
    {
      "id" : "123",
      "description" : "This is a JSON response body",
      "long_value" : 1,
      "bool_value" : true
    }


**Note:** If you change the default `MessageBodyWriter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/FinatraDefaultMessageBodyWriter.scala>`__ implementation (used by the `MessageBodyManager <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/MessageBodyManager.scala>`__)
this will no longer be the default behavior, depending.

You can also always use the `ResponseBuilder`_ to explicitly render a JSON response.

Future Conversion
-----------------

For the basics of Futures in Finatra, see: `Futures <../getting-started/futures.html>`__ in the `Getting Started <../getting-started/futures.html>`__ documentation.

Finatra will convert your route callbacks return type into a `c.t.util.Future[Response]` using the following rules:

-  If you return a `c.t.util.Future[Response]` no conversion will be performed.
-  If you return a `c.t.util.Future[T]` the Future[T] will be mapped to wrap the `T` into an HTTP `200 OK` with `T` as the body.
-  If you return a `scala.concurrent.Future[T]` a Bijection will be attempted to convert the Scala Future into a `c.t.util.Future[T]` then the above case is performed.
-  `Some[T]` will be converted into a HTTP `200 OK`.
-  `None` will be converted into a HTTP `404 NotFound`.
-  Non-response classes will be converted into an HTTP `200 OK` with the class written a the response body.

Callbacks that do not return a `c.t.util.Future <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala>`__ will have their return values wrapped in a `c.t.util.ConstFuture <https://twitter.github.io/util/docs/index.html#com.twitter.util.ConstFuture>`__.

If your non-future result calls a blocking method, you must `avoid blocking the Finagle thread <https://twitter.github.io/scala_school/finagle.html#DontBlock>`__ by wrapping your blocking operation in a FuturePool e.g.

.. code:: scala

    import com.twitter.finatra.utils.FuturePools

    class MyController extends Controller {

      private val futurePool = FuturePools.unboundedPool("CallbackConverter")

      get("/") { request: Request =>
        futurePool {
          blockingCall()
        }
      }
    }


ResponseBuilder
----------------

All HTTP Controllers have a protected `response` field of type `c.t.finatra.http.response.ResponseBuilder <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/response/ResponseBuilder.scala>`__ which can be used to build callback responses.

For example:

.. code:: scala

    get("/foo") { request: Request =>
      ...
      
      response.
        ok.
        header("a", "b").
        json("""
        {
          "name": "Bob",
          "age": 19
        }
        """)
    }

    get("/foo") { request: Request =>
      ...

      response.
        status(999).
        body(bytes)
    }

    get("/redirect") { request: Request =>
      ...

      response
        .temporaryRedirect
        .location("/foo/123")
    }

    post("/users") { request: MyPostRequest =>
      ...

      response
        .created
        .location("/users/123")
    }


For more examples, see the `ResponseBuilderTest <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/response/ResponseBuilderTest.scala>`__.

Cookies:
--------

Cookies, like Headers, are read from request and can set on the response via the `c.t.finatra.http.response.ResponseBuilder <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/response/ResponseBuilder.scala#L151>`__:

.. code:: scala

    get("/") { request =>
      val loggedIn = request.cookies.getValue("loggedIn").getOrElse("false")
      response.ok.
        plain("logged in?:" + loggedIn)
    }

.. code:: scala

    get("/") { request =>
      response.ok.
        plain("hi").
        cookie("loggedIn", "true")
    }


Advanced cookies are supported by creating and configuring `c.t.finagle.http.Cookie <https://github.com/twitter/finagle/blob/develop/finagle-http/src/main/scala/com/twitter/finagle/http/Cookie.scala>`__ objects:

.. code:: scala

    get("/") { request =>
      val c = new Cookie(name = "Biz", value = "Baz")
      c.setSecure(true)
      response.ok.
        plain("get:path").
        cookie(c)
    }


Response Exceptions:
--------------------

Responses can be embedded inside exceptions with `.toException`. You can throw the exception to terminate control flow, or wrap it inside a `Future.exception` to return a failed `Future`.
However, instead of directly returning error responses in this manner, a better convention is to handle application-specific exceptions in an `ExceptionMapper <exceptions.html>`__.

.. code:: scala

    get("/NotFound") { request: Request =>
      response.notFound("abc not found").toFutureException
    }

    get("/ServerError") { request: Request =>
      response.internalServerError.toFutureException
    }

    get("/ServiceUnavailable") { request: Request =>
      // can throw a raw exception too
      throw response.serviceUnavailable.toException
    }

Setting the Response Location Header:
-------------------------------------

`ResponseBuilder`_ has a "location" method.

.. code:: scala

    post("/users") { request: Request =>
      response
        .created
        .location("/users/123")
    }

which can be used:

-  if the URI starts with "http" or "/" then the URI is placed in the Location header unchanged.
-  `response.location("123")` will get turned into the correct full URL by the `HttpResponseFilter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/HttpResponseFilter.scala>`__ (e.g. `http://host.com/users/123`).