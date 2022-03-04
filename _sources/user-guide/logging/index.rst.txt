.. _logging:

Introduction to Logging With Finatra
====================================

Finatra uses the `SLF4J <https://www.slf4j.org/manual.html>`__ API for framework logging. By coupling
the framework to only the `SLF4J <https://www.slf4j.org/manual.html>`__ API, application developers
are free to choose their actual logging implementation.

.. admonition:: From the SLF4J documentation

    "The Simple Logging Facade for Java serves as a simple facade or
    abstraction for various logging frameworks, such as
    java.util.logging, Logback and Log4j. SLF4J allows the end-user to
    plug in the desired logging framework at deployment time."

.. important::

    Logback is the recommended SLF4J-API implementation. As such, `finatra/inject/inject-app <https://github.com/twitter/finatra/tree/develop/inject/inject-app>`__
    and `finatra/inject/inject-server <https://github.com/twitter/finatra/tree/develop/inject/inject-server>`__
    transitively provide SLF4J bridges for the following logging providers:

-  `Log4J 1.x <https://en.wikipedia.org/wiki/Log4j>`__
-  `commons-logging <https://commons.apache.org/proper/commons-logging/>`__
-  `java.util.logging <https://docs.oracle.com/javase/7/docs/api/index.html?java/util/logging/package-summary.html>`__

.. tip::

   Note, `TwitterServer <https://github.com/twitter/twitter-server/blob/b4be9ff71f273a007f880df6ea433f238c2d1f18/server/src/main/scala/com/twitter/server/TwitterServer.scala#L40>`__
   and Finatra's `inject-app <../app/index.html>`__ both mix in the the `c.t.util.logging.Slf4jBridge <https://github.com/twitter/util/blob/develop/util-slf4j-jul-bridge/src/main/scala/com/twitter/util/logging/Slf4jBridge.scala>`__
   trait to attempt installation of the `SLF4JBridgeHandler <https://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html>`__.

   If you are not using one of these and want to bridge JUL to the SLF4J-API, you can mix in the
   `c.t.util.logging.Slf4jBridge <https://github.com/twitter/util/blob/develop/util-slf4j-jul-bridge/src/main/scala/com/twitter/util/logging/Slf4jBridge.scala>`__
   trait to your main application class.

   For more information on the SLF4J bridges see the SLF4J `Bridging legacy APIs <https://www.slf4j.org/legacy.html>`__ documentation.

Since `SLF4J <https://www.slf4j.org/manual.html>`__ is an interface, it requires an actual logging
implementation. However, you should ensure that you **do not** end-up with *multiple* logging
implementations on your classpath, e.g., you should not have multiple SLF4J bindings (`slf4j-nop`,
`slf4j-log4j12`, `slf4j-jdk14`, etc.) nor the `java.util.logging` implementation, etc. on your
classpath as these are all competing implementations and since classpath order is non-deterministic
this will lead to unexpected logging behavior.

While there are several scala-wrappers for SLF4J, Finatra uses the `TwitterUtil <https://twitter.github.io/util/>`__
`util-slf4j-api <https://github.com/twitter/util/tree/develop/util-slf4j-api>`__ project.

The main logging utility is either the `c.t.util.logging.Logging <https://github.com/twitter/util/blob/develop/util-slf4j-api/src/main/scala/com/twitter/util/logging/Logging.scala>`__
trait which can be mixed into any object (or class):

.. code:: scala

    import com.twitter.util.logging.Logging

    object MyObject extends Logging {
      def foo() = {
        info("Calculating...")
        "bar"
      }
    }

or the or the `c.t.util.logging.Logger <https://github.com/twitter/util/blob/develop/util-slf4j-api/src/main/scala/com/twitter/util/logging/Logger.scala>`__
class which provides a standard Logger interface:

.. code:: scala

    import com.twitter.util.logging.Logger

    private object MyCompanionObject {
      val logger: Logger = Logger(this.getClass)
    }

    class MyClass {
      import MyCompanionObject._

      def foo() = {
        logger.info("Calculating...")
        "bar"
      }
    }

For more information see the util-slf4j-api `scaladocs <https://twitter.github.io/util/docs/com/twitter/util/logging/index.html>`__
or `README <https://github.com/twitter/util/blob/develop/util-slf4j-api/README.md>`__.
