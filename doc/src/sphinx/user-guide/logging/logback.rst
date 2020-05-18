.. _logback:

Logback
=======

We highly recommend using `Logback <https://logback.qos.ch/>`__ as an SLF4J binding (logging
implementation).

.. admonition:: From the Logback documentation

  Logback is intended as a successor to the popular log4j project, picking up where log4j leaves off.

  Logback's architecture is sufficiently generic so as to apply under different circumstances. At
  present time, logback is divided into three modules, logback-core, logback-classic and
  logback-access.

See Logback's documentation on `reasons to switch from Log4j <https://logback.qos.ch/reasonsToSwitch.html>`__.

If you choose to use Logback, just include a jar dependency on `ch.qos.logback:logback-classic`. 

E.g., with `sbt <https://www.scala-sbt.org/>`__:

::

    "ch.qos.logback" % "logback-classic" % VERSION


Where, `VERSION` represents the version of Logback you want to use. 

Similarly, with `Maven <https://maven.apache.org/>`__:

.. code:: xml

    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>VERSION</version>
    </dependency>

This will provide the `logback-classic` SLF4J implementation. And when used in addition to the
logging bridges provided transitively through the `finatra/inject-slf4j <https://github.com/twitter/finatra/tree/develop/inject/inject-slf4j>`__
module will configure your SLF4J logging implementation and `correctly bridge all other legacy
APIs <https://www.slf4j.org/legacy.html>`__.

.. note:: To configure dynamic changing log levels with `TwitterServer <https://twitter.github.io/twitter-server/>`__,
    be sure to *also* include a dependency on the TwitterServer Logback library.

    With `sbt <https://www.scala-sbt.org/>`__:

    ::

        "com.twitter" % "twitter-server-logback-classic" % VERSION

    See the TwitterServer `documentation <https://twitter.github.io/twitter-server/Features.html#dynamically-change-log-levels>`__
    for more information on configuration for dynamically changing log levels.


Logback Configuration
---------------------

See the Logback documentation on `configuration <https://logback.qos.ch/manual/configuration.html>`__
for detailed information on configuring Logback logging.

Examples
^^^^^^^^

See the
`logback.xml <https://github.com/twitter/finatra/blob/develop/examples/http-server/scala/src/main/resources/logback.xml>`__
configuration file in the
`http-server <https://github.com/twitter/finatra/tree/develop/examples/http-server>`__
example project for example Logback configurations.

The example
`logback.xml <https://github.com/twitter/finatra/blob/develop/examples/http-server/scala/src/main/resources/logback.xml>`__
configuration file makes use of `Logback's variable substitution <https://logback.qos.ch/manual/configuration.html#variableSubstitution>`__

.. code:: xml

    ${log.access.output:-access.log}
    ${log.service.output:-service.log}

for configuring the `<file/>` output of the appenders. A variable's value is specified at
runtime as a Java system property, e.g.,

.. code:: bash

    -Dlog.service.output=myservice.log

If a value is not provided the inlined default will be used, i.e., what is on the other side of the
`:-`, e.g., `access.log` or `service.log`.

More information about Logback's variable substitution may be found `here <https://logback.qos.ch/manual/configuration.html#variableSubstitution>`__.

Rerouting `java.util.logging`
-------------------------------

Additional configuration is necessary to reroute the `java.util.logging` system. The reason is that
the `jul-to-slf4j` bridge cannot replace classes in the `java.util.logging` package to do the
redirection statically as it does for the other bridge implementations. Instead, it has to register
a handler on the root logger and listen for logging statements like any other handler. It will then
redirect those logging statements appropriately. This redirection is accomplished via SLF4J's
`SLF4JBridgeHandler` [`documentation <https://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html>`__\ ].
`finatra/inject-modules <https://github.com/twitter/finatra/tree/develop/inject/inject-modules>`__
provides a module for initializing this bridge handler via the
`LoggerModule <https://github.com/twitter/finatra/blob/develop/inject/inject-modules/src/main/scala/com/twitter/inject/modules/LoggerModule.scala>`__.
This module is added by the framework to Http and Thrift servers by default.
