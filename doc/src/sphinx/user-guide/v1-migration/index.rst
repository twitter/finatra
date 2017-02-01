.. _v1_migration:

FAQ for upgrading from Finatra v1.x to v2.x
===========================================

Controllers
-----------

You no longer need to return a `Future` from controller routes (however, always return a `Future` if you already have one).

Add Request type to controller callbacks
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: scala

    //v1
    get("/foo") { request =>
    get("/foo") { _ =>

    //v2
    import com.twitter.finagle.http.Request
    get("/foo") { request: Request =>

Change "render" to "response" and specify the HTTP status as the first method after *response* (e.g. ok, created, notFound, etc)

.. code:: scala

    //v1
    render.json(ret)

    //v2
    response.ok.json(ret)

Route params are now stored in request.params (which allows us to reuse `c.t.finagle.http.Request` without defining our own).

.. code:: scala

    //v1
    request.routeParams("q")

    //v2
    request.params("q")

Logging
-------

To continue using "Java Util Logging", add a jar dependency on `slf4j-jdk14 <http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.slf4j%22%20AND%20a%3A%22slf4j-jdk14%22>`__.

Otherwise, we recommend using `Logback <http://logback.qos.ch/>`__ by adding jar dependencies on `ch.qos.logback:logback-classic` and `com.twitter:finatra-slf4j`.

.. code:: scala

    //v1
    log.info("hello")

    //v2
    info("hello")

Exception Mappers
-----------------

ExceptionMappers map exceptions to responses. It needs to implement the following trait:

.. code:: scala

    trait ExceptionMapper[T <: Throwable] {
      def toResponse(request: Request, throwable: T): Response
    }

which says it will handle `T`-typed exceptions. The request that triggered the exception is also provided as an argument. You can make
use of exception mapping by adding the `ExceptionMappingFilter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/ExceptionMappingFilter.scala>`__ to your ``c.t.finatra.routing.HttpRouter``, e.g.,

.. code:: scala

    router.filter[ExceptionMappingFilter[Request]]

The ``ExceptionMappingFilter`` takes an `ExceptionManager <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/exceptions/ExceptionManager.scala>`__ which is provided by default in the `ExceptionManagerModule <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/modules/ExceptionManagerModule.scala>`__.
You can override the default module if necessary by overriding the value in the `HttpServer <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala>`__.

To replicate v1 functionality around `notFound` and `error`, you could do the following:

.. code:: scala

    //v1
    notFound { request => ... }
    error { request => ... }

    //v2 notFound: Create a filter and add it before your controller:
    @Singleton
    class NotFoundFilter @Inject()(
     response: ResponseBuilder)
     extends SimpleFilter[Request, Response] {

     def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
       service(request) map { origResponse =>
         if (origResponse.status == Status.NotFound)
           response.notFound("bar")
         else
           origResponse
       }
     }
    }

    //v2 error: Write an exception mapper and register it with HttpRouter
    @Singleton
    class ArithmeticExceptionMapper @Inject()(
      response: ResponseBuilder)
      extends ExceptionMapper[ArithmeticException] {

      override def toResponse(request: Request, e: ArithmeticException): Response = {
        response.internalServerError("whoops, divide by zero!")
      }
    }

    router.
      filter[NotFoundFilter].
      filter[ExceptionMappingFilter[Request]].
      exceptionMapper[ArithmeticExceptionMapper]

App
---

Override the configure method and add your controllers there.

Note: Flag parsing that used to be in App's constructor should be moved into the `configureHttp` method.

.. code:: scala

    //v2
    class Server extends HttpServer {
      override def configureHttp(router: HttpRouter) {
        val controller1 = new Controller1(...)
        val controller2 = new Controller2(...)
        router.
          commonFilter[CommonFilters].
          commonFilter[NotFoundFilter]. // if needed (see above section on Error Handling)
          add(controller1).
          add(controller2)
      }
    }

Static Files
------------

-  Web resources (html/js) typically go in `src/main/webapp`.
-  Mustache templates now typically go in `src/main/resources/templates`.

To serve static files, you now need explicit routes:

.. code:: scala

    get("/:*") { request: Request =>
     response.ok.file(
       request.params("*"))
    }

If you have an "index" page (e.g. index.html) add the following route.

.. code:: scala

    get("/:*") { request: Request =>
      response.ok.fileOrIndex(
        filePath = request.params("*"),
        indexPath = "index.html")

For more information on serving files in Finatra, see the `Working With Files <../files/index.html#working-with-files>`__ section.

Command Line Flags
------------------

Global flags are no longer used for standard server configuration.
Instead:

::

    //v2
    -log.output=twitter-server.log
    -http.port=:8080
    -admin.port=:8081

Testing
-------

-  In v1, `SpecHelper` and `MockApp` are used.
-  In v2, we provide a common way to run blackbox and whitebox integration tests against a locally running server:

   -  `Simple Example <https://github.com/twitter/finatra/blob/master/examples/hello-world/src/test/scala/com/twitter/hello/HelloWorldFeatureTest.scala>`__

Unsupported v1 Features
-----------------------

-  Render a route from another route.
-  Controller *notFound* and *error* handler methods.
