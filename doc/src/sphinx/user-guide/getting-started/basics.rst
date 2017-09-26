.. _basics:

Dependency Injection
--------------------

Finatra internally uses the Google `Guice <https://github.com/google/guice>`__ dependency injection library extensively which is also available for service writers if they choose to use dependency injection.

**NOTE: You are not required to use Guice dependency injection when using Finatra**. Creating servers, wiring in controllers and applying filters can all be done without using any dependency injection. However, you will not be able to take full-advantage of Finatra's `testing <../testing/index.html>`__ features.

An example of Finatra's dependency-injection integration is adding controllers to Finatra's `HttpRouter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala>`__ *by type*:

.. code:: scala

    class Server extends HttpServer {
      override def configureHttp(router: HttpRouter) {
        router.add[MyController]
      }
    }


As mentioned, it is also possible to do this without using Guice, simply instantiate your controller and add the instance to the router:

.. code:: scala

    class NonDIServer extends HttpServer {
      val myController = new MyController(...)

      override def configureHttp(router: HttpRouter) {
        router.add(myController)
      }
    }