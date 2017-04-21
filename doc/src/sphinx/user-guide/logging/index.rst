.. _logging:

Introduction to Logging With Finatra
====================================

Finatra uses the `SLF4J <http://www.slf4j.org/manual.html>`__ API for framework logging. By coupling the framework to only the
`SLF4J <http://www.slf4j.org/manual.html>`__ API, application developers are free to choose their logging implementation.

.. admonition:: From the SLF4J documentation

    "The Simple Logging Facade for Java serves as a simple facade or
    abstraction for various logging frameworks, such as
    java.util.logging, Logback and log4j. SLF4J allows the end-user to
    plug in the desired logging framework at deployment time."

`finatra/inject/inject-slf4j <https://github.com/twitter/finatra/tree/develop/inject/inject-slf4j>`__ provides transitively SLF4J bridges for the following logging providers:

-  `Log4j <http://en.wikipedia.org/wiki/Log4j>`__
-  `commons-logging <http://commons.apache.org/proper/commons-logging/>`__
-  `java.util.logging <http://docs.oracle.com/javase/7/docs/api/index.html?java/util/logging/package-summary.html>`__: there is a performance penalty for intercepting jul log messages, so `c.t.inject.server.TwitterServer` will install the `SLF4JBridgeHandler <http://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html>`__ which mitigates most of the performance penalty. Note, if you are not using the `c.t.inject.server.TwitterServer` or a subclass, e.g., you are building a command line application directly with `c.t.inject.app.App`, make sure the framework includes the `LoggingModule <https://github.com/twitter/finatra/blob/develop/inject/inject-modules/src/main/scala/com/twitter/inject/modules/LoggerModule.scala>`__ as a framework module.

Since `SLF4J <http://www.slf4j.org/manual.html>`__ is an interface, it  requires an actual logging implementation. However, you should ensure that you **do not** end-up with *multiple* logging implementations on your classpath, e.g., you should not have multiple SLF4J bindings (`slf4j-nop`, `slf4j-log4j12`, `slf4j-jdk14`, etc.) nor a `java.util.logging` implementation, etc. on your classpath as these are all competing implementations and since classpath order is non-deterministic this will lead to unexpected logging behavior.

While there are several scala-wrappers for SLF4J, Finatra uses and exposes some additional features on top of the `TwitterUtil <https://twitter.github.io/util/>`__ `util-slf4j-api <https://github.com/twitter/util/tree/develop/util-slf4j-api>`__ project.

The main logging utility is the `c.t.inject.Logging <https://github.com/twitter/finatra/blob/develop/inject/inject-slf4j/src/main/scala/com/twitter/inject/Logging.scala>`__
trait which can be mixed into any object or class:

.. code:: scala

    import com.twitter.inject.Logging

    class MyClass extends Logging {
      def foo() = {
        info("Calculating...")
        "bar"
      }
    }


This trait is a wrapper with some added utility over the `c.t.util.logging.Logging <https://github.com/twitter/util/blob/develop/util-slf4j-api/src/main/scala/com/twitter/util/logging/Logging.scala>`__.

Scala users should prefer using the logging methods of the `c.t.inject.Logging <https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/main/scala/com/twitter/inject/Logging.scala>`__ trait (as opposed to directly accessing the Logger instance) as these methods use "call-by-name" parameters.

For more information see the `scaladocs <https://twitter.github.io/finatra/scaladocs/index.html#com.twitter.inject.Logging>`__ for `c.t.inject.Logging` or the `util-slf4j-api README <https://github.com/twitter/util/blob/develop/util-slf4j-api/README.md>`__.
