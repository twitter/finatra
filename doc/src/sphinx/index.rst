Finatra
=======

|Build Status| |Test Coverage| |Project status| |Maven Central| |Gitter|

Finatra builds on `TwitterServer <https://twitter.github.io/twitter-server/>`__ and uses `Finagle <https://twitter.github.io/finagle/guide/>`__, therefore it is *highly recommended* that you familiarize yourself with those frameworks before getting started.

The version of Finatra documented here is **version 2.x**. Version 2.x is a **complete rewrite** over v1.x and as such many things are different.

For high-level information about the changes from v1.x see the blog post `here <https://blog.twitter.com/2015/finatra-20-the-fast-testable-scala-services-framework-that-powers-twitter>`__.

Finatra at its core is agnostic to the type of service or application being created. It can be used to build anything based on `TwitterUtil <https://github.com/twitter/util>`__: `c.t.app.App <https://github.com/twitter/util/blob/develop/util-app/src/main/scala/com/twitter/app/App.scala>`__. 

For servers, Finatra builds on top of the `features <https://twitter.github.io/twitter-server/Features.html>`__ of `TwitterServer <https://twitter.github.io/twitter-server/>`__ (and `Finagle <https://twitter.github.io/finagle>`__) by allowing you to easily define a `Server <https://twitter.github.io/finagle/guide/Servers.html>`__ and controllers (a `Service <https://twitter.github.io/finagle/guide/ServicesAndFilters.html#services>`__-like abstraction) which define and handle endpoints of the Server. You can also compose `Filters <https://twitter.github.io/finagle/guide/ServicesAndFilters.html#filters>`__ either per controller, per route in a controller, or across all controllers.

Getting Started
---------------

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

To continue getting started, please see the `Finatra User's Guide <user-guide/index.html>`__.

Useful Links
------------

.. toctree::

  user-guide/index
  Scaladocs <https://twitter.github.io/finatra/scaladocs/index.html>
  Finagle Blog <https://finagle.github.io/blog/>
  presentations/index
  Forum <https://groups.google.com/forum/#!forum/finatra-users>
  Changelog

Contributing
------------

Finatra is an open source project that welcomes contributions from the greater community. We’re thankful for the many people who have already contributed and if you’re interested, please read the `contributing <https://github.com/twitter/finatra/blob/develop/CONTRIBUTING.md>`__ guidelines.

For support feel free to follow and/or tweet at the `@finatra <https://twitter.com/finatra>`__ Twitter account, post questions to the `Gitter chat room <https://gitter.im/twitter/finatra>`__, or email the finatra-users Google group: `finatra-users@googlegroups.com <mailto:finatra-users@googlegroups.com>`__.


.. |Build Status| image:: https://secure.travis-ci.org/twitter/finatra.png?branch=develop 
   :target: http://travis-ci.org/twitter/finatra?branch=develop
.. |Test Coverage| image:: http://codecov.io/github/twitter/finatra/coverage.svg?branch=develop 
   :target: http://codecov.io/github/twitter/finatra?branch=develop
.. |Project status| image:: https://img.shields.io/badge/status-active-brightgreen.svg 
   :target: https://github.com/twitter/finatra#status
.. |Maven Central| image:: https://maven-badges.herokuapp.com/maven-central/com.twitter/finatra-http_2.12/badge.svg
.. |Gitter| image:: https://badges.gitter.im/Join%20Chat.svg 
   :target: https://gitter.im/twitter/finatra
