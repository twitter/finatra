.. _mdc:

Mapped Diagnostic Context (MDC) Support
---------------------------------------

Finatra offers an integration with the SLF4J API `Mapped Diagnostic Context <https://www.slf4j.org/manual.html#mdc>`__
for consistent logging of useful information.

.. admonition:: From the SLF4J API `documentation <https://www.slf4j.org/manual.html#mdc>`__

    "Mapped Diagnostic Context" is essentially a map maintained by the logging
    framework where the application code provides key-value pairs which can then
    be inserted by the logging framework in log messages. MDC data can also be
    highly helpful in filtering messages or triggering certain actions.

Usage
-----

In order to use the Mapped Diagnostic Context (MDC), an application must first initialize the
framework MDC `Finagle <https://twitter.github.io/finagle/>`__ adapter,
`FinagleMDCAdapter <https://github.com/twitter/finatra/blob/develop/inject/inject-slf4j/src/main/scala/com/twitter/inject/logging/FinagleMDCAdapter.scala>`__
provided by Finatra. This is done by calling `c.t.inject.logging.MDCInitializer.init()`.

The MDC can be closed over the execution of a function, meaning an empty MDC map will be created at
the beginning of the closure and then discard after the closure. When used in a Filter in the request
path this means that MDC logging can be scoped per-request.

For example:

.. code:: scala

    import com.twitter.finagle.{Service, Filter}
    import com.twitter.inject.logging.MDCInitializer
    import com.twitter.util.Future

    class MDCInitializationFilter extends Filter[Request, Response, Request, Response] {
      /* Initialize MDC adapter which overrides the standard one */
      MDCInitializer.init()

      override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
        MDCInitializer.let {
          service(request)
        }
      }
    }


The `Service[Request, Response]` executed by the Filter can then get and set MDC values which can be
used by the chosen logging implementation's MDC configuration.  For example, see the
`Logback MDC documentation <http://logback.qos.ch/manual/mdc.html>`__ for more information the
MDC functionality and configuration in Logback.


`LoggingMDCFilter`
------------------

This MDC Filter initialization integration is already provided by either the http `LoggingMDCFilter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/LoggingMDCFilter.scala>`__
or the thrift `LoggingMDCFilter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/filters/LoggingMDCFilter.scala>`__
which can then be included in your server Filter chain.

.. important::

    Make sure the `LoggingMDCFilter` is placed **before** any other Filters or code which will add
    MDC entries or which will expect MDC entries to be present.

Example
-------

The `hello-world <https://github.com/twitter/finatra/tree/develop/examples/hello-world>`__
example Logback `configuration <https://github.com/twitter/finatra/blob/c6e4716f082c0c8790d06d9e1664aacbd0c3fede/examples/hello-world/src/main/resources/logback.xml#L25>`__
references the contextual key `traceId` which will be logged with every statement sent to a Logback
`Appender <https://logback.qos.ch/manual/appenders.html>`__. It is the responsibility of the
application to populate the MDC with this contextual information. In this case, the `traceId` is
added by including the http `TraceIdMDCLoggingFilter <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/TraceIdMDCFilter.scala>`__
(there is also a thrift `TraceIdMDCLoggingFilter <https://github.com/twitter/finatra/blob/develop/thrift/src/main/scala/com/twitter/finatra/thrift/filters/TraceIdMDCFilter.scala>`__).

In a server definition, the `LoggingMDCFilter` must be listed before the `TraceIdMDCLoggingFilter`,
e.g.,

.. code:: scala

    import com.twitter.finagle.http.{Request, Response}
    import com.twitter.finatra.http.HttpServer
    import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
    import com.twitter.finatra.http.routing.HttpRouter

    object HelloWorldServerMain extends HelloWorldServer

    class HelloWorldServer extends HttpServer {

      override def configureHttp(router: HttpRouter) {
        router
          .filter[LoggingMDCFilter[Request, Response]]
          .filter[TraceIdMDCFilter[Request, Response]]
          .add[HelloWorldController]
      }
    }




