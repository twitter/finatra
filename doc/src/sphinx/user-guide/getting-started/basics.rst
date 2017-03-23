.. _basics:

First Steps
===========

To get started, add a dependency on either `finatra-http <http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20a%3A%22finatra-http_2.12%22>`__ or `finatra-thrift <http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20a%3A%22finatra-thrift_2.12%22>`__ depending if you are building an HTTP or Thrift server.

E.g., with `sbt <http://www.scala-sbt.org/>`__:

.. parsed-literal::

    "com.twitter" %% "finatra-http" % "\ |release|\ "

or

.. parsed-literal::

    "com.twitter" %% "finatra-thrift" % "\ |release|\ "

Or similarily with `Maven <http://maven.apache.org/>`__:

.. parsed-literal::

    <dependency>
      <groupId>com.twitter</groupId>
      <artifactId>finatra-http_2.12</artifactId>
      <version>\ |release|\ </version>
    </dependency>

or

.. parsed-literal::

    <dependency>
      <groupId>com.twitter</groupId>
      <artifactId>finatra-thrift_2.12</artifactId>
      <version>\ |release|\ </version>
    </dependency>

Note: with Maven, you **must** append the appropriate scala version to the artifact name (e.g., `_2.12`). 

See the Finatra `hello-world <https://github.com/twitter/finatra/tree/finatra-2.2.0/examples/hello-world>`__ example for a more in-depth example.

Test Dependencies
-----------------

Finatra publishes `test-jars <https://maven.apache.org/guides/mini/guide-attached-tests.html>`__ for most modules. The test-jars include re-usable utilities for use in testing (e.g., the `EmbeddedTwitterServer <https://github.com/twitter/finatra/blob/develop/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala>`__).

To add a test-jar dependency, depend on the appropriate module with the `tests` classifier. Additionally, these dependencies are typically **only needed** in the ``test`` scope for your project. E.g., with `sbt <http://www.scala-sbt.org/>`__:

.. parsed-literal::

    "com.twitter" %% "finatra-http" % "\ |release|\ " % "test" classifier "tests"

or

.. parsed-literal::

    "com.twitter" %% "finatra-thrift" % "\ |release|\ " % "test" classifier "tests"

See the `sbt <http://www.scala-sbt.org/>`__ documentation for more information on using `ivy configurations and classifiers <http://www.scala-sbt.org/0.13/docs/Library-Management.html>`__.

And with `Maven <http://maven.apache.org/>`__:

.. parsed-literal::

    <dependency>
      <groupId>com.twitter</groupId>
      <artifactId>finatra-http_2.12</artifactId>
      <scope>test</scope>
      <type>test-jar</type>
      <version>\ |release|\ </version>
    </dependency>

or

.. parsed-literal::

    <dependency>
      <groupId>com.twitter</groupId>
      <artifactId>finatra-thrift_2.12</artifactId>
      <scope>test</scope>
      <type>test-jar</type>
      <version>\ |release|\ </version>
    </dependency>

There is a `downside <https://maven.apache.org/plugins/maven-jar-plugin/examples/create-test-jar.html>`__ to publishing test-jars in this manner: transitive test-scoped dependencies are not resolved. Maven and sbt only resolve compile-time dependencies transitively, so you will need to specify all other required test-scoped dependencies manually.

For example, the `finatra-http` test-jar depends on the `inject-app` test-jar (among others). You will have to **manually add** a dependency on the `inject-app` test-jar when using the `finatra-http` test-jar since the `inject-app` test-jar will not be resolvedtransitively. 

Using the `sbt-dependency-graph <https://github.com/jrudolph/sbt-dependency-graph>`__ plugin, you can list the dependencies of the `finatra-http` test configuration for the `packageBin` task:

.. code:: bash

    $ ./sbt -Dsbt.log.noformat=true http/test:packageBin::dependencyList 2>&1 | grep 'com\.twitter:finatra\|com\.twitter:inject'
    [info] com.twitter:finatra-http_2.12:...
    [info] com.twitter:finatra-httpclient_2.12:...
    [info] com.twitter:finatra-jackson_2.12:...
    [info] com.twitter:finatra-slf4j_2.12:...
    [info] com.twitter:finatra-utils_2.12:...
    [info] com.twitter:inject-app_2.12:...
    [info] com.twitter:inject-core_2.12:...
    [info] com.twitter:inject-modules_2.12:...
    [info] com.twitter:inject-request-scope_2.12:...
    [info] com.twitter:inject-server_2.12:...
    [info] com.twitter:inject-slf4j_2.12:...
    [info] com.twitter:inject-utils_2.12:...

In this case, when executing the `packageBin` task for `finatra-http` in the test configuration these dependencies are necessary. Unfortunately, this listing does not explicity state if it's the compile-time or the test-jar version of the dependency that is necessary. However, it is safe to assume that if you want a dependency on the `finatra-http` test-jar you will also need to add dependencies on any test-jar from the listed dependencies as well.

`Lightbend Activator <https://www.lightbend.com/activator/download>`__
----------------------------------------------------------------------

Finatra also has Lightbend Activator `templates <https://www.lightbend.com/activator/templates#filter:finatra%20v2.x>`__ for project generation:

-  `HTTP template <https://github.com/twitter/finatra-activator-http-seed>`__ - instructions `here <https://www.lightbend.com/activator/template/finatra-http-seed>`__.
-  `Thrift template <https://github.com/twitter/finatra-activator-thrift-seed>`__ - instructions `here <https://www.lightbend.com/activator/template/finatra-thrift-seed>`__.

See the Lightbend Activator `documentation <https://www.lightbend.com/activator/docs>`__ for information how to use these templates with the activator application.

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