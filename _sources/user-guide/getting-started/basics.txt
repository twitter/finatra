.. _basics:

Dependency Injection
--------------------

The Finatra framework uses `Dependency Injection <https://en.wikipedia.org/wiki/Dependency_injection>`_ (DI) and it is important to understand the concept -- specifically how it relates to effective testing which helps to explain the motivations of the framework.

`Dependency Injection <https://en.wikipedia.org/wiki/Dependency_injection>`_ is one way to implement the `Inversion of Control (IoC) <https://en.wikipedia.org/wiki/Inversion_of_control>`_ programming principle. More importantly `Dependency Injection <https://en.wikipedia.org/wiki/Dependency_injection>`_ is a **design pattern** and does not refer to any specific implementation of a library.

Finatra does use the Google `Guice <https://github.com/google/guice>`__ `Dependency Injection <https://en.wikipedia.org/wiki/Dependency_injection>`_ library which is also available for service writers if they choose to use `Dependency Injection <https://en.wikipedia.org/wiki/Dependency_injection>`_. However, the framework was designed around the principle of `Dependency Injection <https://en.wikipedia.org/wiki/Dependency_injection>`_, not the `Guice <https://github.com/google/guice>`_ library implementation with a primary goal to build testability of code into the framework.

With that, a great place to start on understanding the reasoning behind `Dependency Injection <https://en.wikipedia.org/wiki/Dependency_injection>`_  is the `Motivation <https://github.com/google/guice/wiki/Motivation>`__ section of the Google `Guice <https://github.com/google/guice>`__ framework.

.. attention::
  You are **not required** to use Google `Guice <https://github.com/google/guice>`__ `Dependency Injection <https://en.wikipedia.org/wiki/Dependency_injection>`_ when using Finatra. Creating servers, wiring in controllers and applying filters can all be done without using any dependency injection. However, you will not be able to take full-advantage of Finatra's `testing <../testing/index.html>`__ features.

A simple example of Finatra's `Dependency Injection <https://en.wikipedia.org/wiki/Dependency_injection>`_  integration is adding controllers to Finatra's `HttpRouter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/routing/HttpRouter.scala>`__ *by type*:

.. code:: scala

    class Server extends HttpServer {
      override def configureHttp(router: HttpRouter) {
        router.add[MyController]
      }
    }

As mentioned, it is also possible to do this without using `Guice <https://github.com/google/guice>`__: simply instantiate your controller and add the instance to the router:

.. code:: scala

    class NonDIServer extends HttpServer {
      val myController = new MyController(...)

      override def configureHttp(router: HttpRouter) {
        router.add(myController)
      }
    }

Dependency Injection and Testing
--------------------------------

There are many resources around this topic but we recommend taking a look at `The Tao of Testing: Chapter 3 - Dependency Injection <https://jasonpolites.github.io/tao-of-testing/ch3-1.1.html>`__ as a primer for how `Dependency Injection <https://en.wikipedia.org/wiki/Dependency_injection>`_ can help to write more testable code.
