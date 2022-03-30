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
    -  `java.util.logging <https://docs.oracle.com/javase/7/docs/api/index.html?java/util/logging/package-summary.html>`__ (JUL)

Bridging Logging Implementations
--------------------------------

For background see the SLF4J `Bridging legacy APIs <https://www.slf4j.org/legacy.html>`__ documentation.

`TwitterServer <https://github.com/twitter/twitter-server/blob/b4be9ff71f273a007f880df6ea433f238c2d1f18/server/src/main/scala/com/twitter/server/TwitterServer.scala#L40>`__
and Finatra's `inject-app <../app/index.html>`__ both mix in the the `c.t.util.logging.Slf4jBridge <https://github.com/twitter/util/blob/develop/util-slf4j-jul-bridge/src/main/scala/com/twitter/util/logging/Slf4jBridge.scala>`__
trait to attempt installation of the `SLF4JBridgeHandler <https://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html>`__.

If you are not using one of these and want to bridge `JUL <https://docs.oracle.com/javase/7/docs/api/index.html?java/util/logging/package-summary.html>`__
to the SLF4J-API, you can mix in the `c.t.util.logging.Slf4jBridge <https://github.com/twitter/util/blob/develop/util-slf4j-jul-bridge/src/main/scala/com/twitter/util/logging/Slf4jBridge.scala>`__
trait to your main application class.

Since `SLF4J <https://www.slf4j.org/manual.html>`__ is an interface, it requires an actual logging
implementation. However, you should ensure that you **do not** end-up with *multiple* logging
implementations on your classpath, e.g., you should not have multiple SLF4J bindings (`slf4j-nop`,
`slf4j-log4j12`, `slf4j-jdk14`, etc.) nor the `java.util.logging` implementation, etc. on your
classpath as these are all competing implementations and since classpath order is non-deterministic,
this will lead to unexpected logging behavior.

`util/util-slf4j-api`
---------------------

While there are several scala-wrappers for SLF4J, Finatra uses the `TwitterUtil <https://twitter.github.io/util/>`__
`util-slf4j-api <https://github.com/twitter/util/tree/develop/util-slf4j-api>`__ project.

There are two main utilities in the library: the `c.t.util.logging.Logging <https://github.com/twitter/util/blob/develop/util-slf4j-api/src/main/scala/com/twitter/util/logging/Logging.scala>`__
trait and the `c.t.util.logging.Logger <https://github.com/twitter/util/blob/develop/util-slf4j-api/src/main/scala/com/twitter/util/logging/Logger.scala>`__
class which provides a standard "Logger" interface that wraps the SLF4J `org.slf4j.Logger <https://www.javadoc.io/static/org.slf4j/slf4j-api/1.7.25/index.html?org/slf4j/Logger.html>`__
but with API ergonomics for Scala users and other optimizations.

`c.t.util.logging.Logging`
~~~~~~~~~~~~~~~~~~~~~~~~~~

The `c.t.util.logging.Logging <https://github.com/twitter/util/blob/develop/util-slf4j-api/src/main/scala/com/twitter/util/logging/Logging.scala>`__
trait can be mixed into any object or class:

.. code-block:: scala
   :emphasize-lines: 3,5

    import com.twitter.util.logging.Logging

    object MyObject extends Logging {
      def foo() = {
        info("Calculating...")
        "bar"
      }
    }

When mixing in this trait, a new `c.t.util.logging.Logger` `is instantiated <https://github.com/twitter/util/blob/20a66daa41117e473b9d1cba2b2a459efca000af/util-slf4j-api/src/main/scala/com/twitter/util/logging/Logging.scala#L59>`__
for the current class. The trait exposes logging methods which can be called as though they exist
within the current class, e.g., `trace`, `debug`, `info`, `warn`, or `error`.

See the `c.t.util.logging.Logging` trait `scaladocs <https://twitter.github.io/util/docs/com/twitter/util/logging/Logging.html>`__
for details.

As an example, in the code above, by mixing in the `Logging` trait the `MyObject` now has a logger named
for the `MyObject.getClass` class. E.g.,

.. code-block:: scala
    :emphasize-lines: 36, 63

    Welcome to Scala 2.12.15 (JDK 64-Bit Server VM, Java 1.8.0_275).
    Type in expressions for evaluation. Or try :help.

    scala> import com.twitter.util.logging.Logging
    import com.twitter.util.logging.Logging

    scala> import com.twitter.util.logging.Logger
    import com.twitter.util.logging.Logger

    scala> object MyObject extends Logging {
         |
         |   def myLogger: Logger = this.logger
         |
         |   def myLoggerName: String = this.loggerName
         |
         |   def foo() = {
         |     info("Calculating...")
         |     "bar"
         |   }
         | }
    defined object MyObject

    scala> MyObject.myLoggerName
    res0: String = MyObject

    scala> val logger = MyObject.myLogger
    logger: com.twitter.util.logging.Logger = com.twitter.util.logging.Logger@65da4a5d

    scala> logger.name
    res1: String = MyObject

    scala> logger.isInfoEnabled
    res2: Boolean = true

    scala> MyObject.foo
    12:41:20.171 [main] INFO $line5.$read$$iw$$iw$MyObject - Calculating...
    res3: String = bar

    scala> class MyClass extends Logging {
         |
         |   def myLogger: Logger = this.logger
         |
         |   def myLoggerName: String = this.loggerName
         |
         |   def bar() = {
         |     info("Calculating...")
         |     "foo"
         |   }
         |
         | }
    defined class MyClass

    scala> val c = new MyClass
    c: MyClass = MyClass@eaa901a

    scala> c.myLoggerName
    res4: String = MyClass

    scala> val logger = c.myLogger
    logger: com.twitter.util.logging.Logger = com.twitter.util.logging.Logger@4a03c4bc

    scala> c.bar
    12:43:19.130 [main] INFO $line15.$read$$iw$$iw$MyClass - Calculating...
    res5: String = foo

    scala>

.. note::

    The `c.t.util.logging.Logger <https://github.com/twitter/util/blob/develop/util-slf4j-api/src/main/scala/com/twitter/util/logging/Logger.scala>`__
    will `remove the trailing <https://github.com/twitter/util/blob/20a66daa41117e473b9d1cba2b2a459efca000af/util-slf4j-api/src/main/scala/com/twitter/util/logging/Logger.scala#L70>`__
    `$` from `Scala object <https://docs.scala-lang.org/tour/singleton-objects.html>`__ class names.

`c.t.util.logging.Logger`
~~~~~~~~~~~~~~~~~~~~~~~~~

The `c.t.util.logging.Logger <https://github.com/twitter/util/blob/develop/util-slf4j-api/src/main/scala/com/twitter/util/logging/Logger.scala>`__
class provides a standard "Logger" interface wrapping the SLF4J `org.slf4j.Logger <https://www.javadoc.io/static/org.slf4j/slf4j-api/1.7.25/index.html?org/slf4j/Logger.html>`__:

.. code-block:: scala
   :emphasize-lines: 4, 11

    import com.twitter.util.logging.Logger

    private object MyCompanion {
      val logger: Logger = Logger(this.getClass)
    }

    class MyCompanion {
      import MyCompanion._

      def foo() = {
        logger.info("Calculating...")
        "bar"
      }
    }

.. code-block:: scala
   :emphasize-lines: 32

    Welcome to Scala 2.12.15 (JDK 64-Bit Server VM, Java 1.8.0_275).
    Type in expressions for evaluation. Or try :help.

    scala> import com.twitter.util.logging.Logger
    import com.twitter.util.logging.Logger

    scala> :paste
    // Entering paste mode (ctrl-D to finish)

    private object MyCompanion {
      val logger: Logger = Logger(this.getClass)
    }

    class MyCompanion {
      import MyCompanion._

      def foo() = {
        logger.info("Calculating...")
        "bar"
      }
    }

    // Exiting paste mode, now interpreting.

    defined object MyCompanion
    defined class MyCompanion

    scala> val c = new MyCompanion
    c: MyCompanion = MyCompanion@63cf578f

    scala> c.foo
    12:52:43.809 [main] INFO $line4.$read$$iw$$iw$MyCompanion - Calculating...
    res0: String = bar

    scala>

For more information see the util-slf4j-api `scaladocs <https://twitter.github.io/util/docs/com/twitter/util/logging/index.html>`__
or `README <https://github.com/twitter/util/blob/develop/util-slf4j-api/README.md>`__.
