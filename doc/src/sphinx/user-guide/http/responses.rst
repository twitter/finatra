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

For the basics of Futures in Finatra, see: `Futures <../getting-started/futures.html>`__ in the `Getting Started <../index.html#getting-started>`__ documentation.

Conversion Rules
^^^^^^^^^^^^^^^^

Finatra will attempt to convert your route callback's return type into a `c.t.util.Future[Response]` using the following rules:

-  If you return a `Future[Response]` no conversion will be performed.
-  If you return a `Future[T]` it will be mapped to wrap the `T` into an HTTP `200 OK` Response, with `T` as the body.
-  If you return a `scala.concurrent.Future[T]` a Bijection will be attempted to convert the Scala Future into a `Future[T]` then the above case is performed.
-  `Some[T]` will be converted into a HTTP `200 OK`.
-  `None` will be converted into a HTTP `404 NotFound`.
-  Non-Response classes will be converted into an HTTP `200 OK` with the class written as the response body.

Non-Future Callback Return Types
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Callbacks that do not return a `c.t.util.Future <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala>`__ will have their return values wrapped in a `c.t.util.ConstFuture <https://twitter.github.io/util/docs/index.html#com.twitter.util.ConstFuture>`__ (which does no asynchronous work) for the purpose of satisfying types only.

Don't Block
^^^^^^^^^^^

If you are not returning a Future from your callback and it does synchronous work or calls a blocking method, you must `avoid blocking the Finagle thread <https://twitter.github.io/scala_school/finagle.html#DontBlock>`__ by wrapping your blocking operation in a `FuturePool <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/FuturePool.scala>`__ e.g.

.. code:: scala

    import com.twitter.finatra.utils.FuturePools

    class MyController extends Controller {

      private val futurePool = FuturePools.unboundedPool("CallbackConverter")

      get("/") { request: Request =>
        futurePool {
          blockingOperation()
        }
      }
    }

More information on blocking in Finagle can be found `here <https://finagle.github.io/blog/2016/09/01/block-party/>`__.

ResponseBuilder
----------------

All HTTP Controllers have a protected `response` field of type `c.t.finatra.http.response.ResponseBuilder <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/response/ResponseBuilder.scala>`__ which can be used to build a `c.t.finagle.http.Response` in your Controller route callback functions.

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

    get("/foo/future") { request: Request =>
      ...

      val futureOpResult: Future[Bar] = ...
      futureOpResult.map { result =>
        response
          .ok
          .body(result)
      }
    }

    post("/users") { request: MyPostRequest =>
      ...

      response
        .created
        .location("/users/123")
    }


For more examples, see the `ResponseBuilderTest <https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/response/ResponseBuilderTest.scala>`__.

Wait, how do I create a `Response` from a `Future[T]`?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As noted in the `Future Conversion`_ section, Finatra will attempt to construct a proper return type of `Future[Response]` from your callback's return type. Though, in many cases, you may find that you have a `Future[T]` and want to translate this into a `c.t.finagle.http.Response` yourself using the `ResponseBuilder`_. 

Constructing a response is synchronous, thus the `ResponseBuilder`_ has no concept of Futures. However, the `ResponseBuilder`_
is meant to be somewhat generic so its API for constructing a response body accepts an `Any` type which may make it *seem like* it should work to simply put in a `Future[T]` into the body. However, this is incorrect.

If you have a `Future[T]` and want to return a `c.t.finagle.http.Response` you should either:

- convert it to a `Future[Response]` or 
- do nothing and let the Finatra `CallbackConverter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/CallbackConverter.scala#L139>`__ convert the  `Future[T]` to an HTTP `200 OK` with `T` as the body (as mentioned in `Future Conversion`_ section above).

To convert a `Future[T]` to a `Future[Response]`, you would use `Future#map <https://twitter.github.io/effectivescala/#Twitter's%20standard%20libraries-Futures>`__:

.. code:: scala

    get("/foo") { request: Request => 
      val futureResult: Future[Foo] = ... // a call that returns a Future[Foo]

      // map the Future[T] to create a Future[Response]
      futureResult.map { result: Foo =>
        // construct your response here using the ResponseBuilder
        response.ok.body(result)
      }
    }
    

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