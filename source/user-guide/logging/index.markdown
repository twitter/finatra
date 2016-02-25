---
layout: user_guide
title: "Logging"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li class="active">Logging</li>
</ol>

Finatra uses the [SLF4J](http://www.slf4j.org/manual.html) api for framework logging. By coupling the framework to only the [SLF4J](http://www.slf4j.org/manual.html) api, application developers are free to choose their logging implementation.

##### From the SLF4J documentation:
> "The Simple Logging Facade for Java serves as a simple facade or abstraction for various logging frameworks, such as java.util.logging, Logback and log4j. SLF4J allows the end-user to plug in the desired logging framework at deployment time."

[`finatra-slf4j`](https://github.com/twitter/finatra/tree/master/slf4j) provides transitively SLF4J bridges for the following logging providers:

  * [Log4j](http://en.wikipedia.org/wiki/Log4j)
  * [commons-logging](http://commons.apache.org/proper/commons-logging/)
  * jdk14-logging
  * [java.util.logging](http://docs.oracle.com/javase/7/docs/api/index.html?java/util/logging/package-summary.html): There is a performance penalty for intercepting jul log messages, so make sure the framework includes the [Slf4jBridgeModule](https://github.com/twitter/finatra/tree/master/slf4j/src/main/scala/com/twitter/finatra/logging/modules/Slf4jBridgeModule.scala) as a framework module. This will install the [SLF4JBridgeHandler](http://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html) which mitigates most of the performance penalty.

## <a class="anchor" name="basics" href="#basics">Basics</a>
===============================

Since [SLF4J](http://www.slf4j.org/manual.html) is an interface, it requires an actual logging implementation. However, you should ensure that you do not end-up with *multiple* logging implementations on your classpath, e.g., you should not have multiple SLF4J bindings (`slf4j-nop`, `slf4j-log4j12`, `slf4j-jdk14`, etc.) and/or a `java.util.logging` implementation, etc. on your classpath as these are all competing implementations and since classpath order is non-deterministic this will lead to unexpected logging behavior.

While there are several scala-wrappers for SLF4J, Finatra uses and exposes some additional features on top of the excellent [grizzled-slf4j](http://software.clapper.org/grizzled-slf4j/) project.

The main logging utility is the [com.twitter.inject.Logging](https://github.com/twitter/finatra/tree/master/inject/inject-core/src/main/scala/com/twitter/inject/Logging.scala) trait which can be mixed into any object or class:

```scala

import com.twitter.inject.Logging

class MyClass extends Logging {
  def foo() = {
    info("Calculating...")
    "bar"
  }
}
```
<div></div>

This trait is a wrapper over the `grizzled.slf4j.Logger`[[documentation](http://software.clapper.org/grizzled-slf4j/api/#grizzled.slf4j.Logger)] and adds some utility methods on top of the `grizzled.slf4j.Logger`. For more information see the [scaladoc](/finatra/docs/index.html#com.twitter.inject.Logging) for `com.twitter.inject.Logging`.

## <a class="anchor" name="logback" href="#logback">Logback</a>
===============================

We highly recommend using [Logback](http://logback.qos.ch/) as an SLF4J binding (logging implementation). If you choose to use Logback, just include a jar dependency on `ch.qos.logback:logback-classic`. This will provide a Logback SLF4J implementation (`logback-classic`) in addition to the logging bridges provided transitively through the [`finatra/slf4j`](https://github.com/twitter/finatra/tree/master/slf4j) module.

### Configuration

See the [logback.xml](https://github.com/twitter/finatra/tree/master/examples/hello-world/src/main/resources/logback.xml) and [logback-test.xml](.https://github.com/twitter/finatra/tree/master/examples/hello-world/src/test/resources/logback-test.xml) configuration files in the [hello-world](https://github.com/twitter/finatra/tree/master/examples/hello-world) example project for example Logback configurations.

See the [Logback documentation on configuration](http://logback.qos.ch/manual/configuration.html) for more information.

The example [logback.xml](https://github.com/twitter/finatra/tree/master/examples/hello-world/src/main/resources/logback.xml) configuration file makes use of `${log.access.output:-access.log}` and `${log.service.output:-service.log}` variables for configuring the `<file/>` output of two appenders. A variable's value is specified at runtime as a Java system property (e.g., `-Dlog.service.output=myservice.log`). If a value is not provided the inlined default will be used, i.e., what is on the other side of the `:-`, e.g., `service.log`.

More information about Logback's variable substitution may be found here:&nbsp;&nbsp;http://logback.qos.ch/manual/configuration.html#variableSubstitution

### Rerouting `java.util.logging`

Additional configuration is necessary to reroute the `java.util.logging` system. The reason is that the `jul-to-slf4j` bridge cannot replace classes in the `java.util.logging` package to do the redirection statically as it does for the other bridge implementations. Instead, it has to register a handler on the root logger and listen for logging statements like any other handler. It will then redirect those logging statements appropriately. This redirection is accomplished via SLF4J's `SLF4JBridgeHandler` [[documentation](http://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html)]. [`finatra/slf4j`](https://github.com/twitter/finatra/tree/master/slf4j) provides a module for initializing this bridge handler via the [Slf4jBridgeModule](https://github.com/twitter/finatra/blob/master/slf4j/src/main/scala/com/twitter/finatra/logging/modules/Slf4jBridgeModule.scala). This module is added by the framework to Http and Thrift servers by default.


## <a class="no-pad-anchor" name="mdc" href="#mdc">Mapped Diagnostic Context Filter</a>
===============================

Finatra offers an integration with Logback's [Mapped Diagnostic Context](http://logback.qos.ch/manual/mdc.html) (MDC) for consistent logging of useful information. For example, the [hello-world configuration](https://github.com/twitter/finatra/blob/master/examples/hello-world/src/main/resources/logback.xml#L19) references the contextual key "traceId" which will be logged with every statement sent to the appender. It is the responsibility of the application to populate the MDC with this contextual information. In this case, the "traceId" is added by including either the http  [TraceIdMDCLoggingFilter](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/filters/TraceIdMDCFilter.scala) or thrift [TraceIdMDCLoggingFilter](https://github.com/twitter/finatra/blob/master/thrift/src/main/scala/com/twitter/finatra/thrift/filters/TraceIdMDCFilter.scala).

In order to use the Mapped Diagnostic Context, an application must first initialize the MDC [Finagle](http://twitter.github.io/finagle/) adapter provided by Finatra. This initialization is provided by including either the http [LoggingMDCFilter](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/filters/LoggingMDCFilter.scala#L14) or thrift [LoggingMDCFilter](https://github.com/twitter/finatra/blob/master/thrift/src/main/scala/com/twitter/finatra/thrift/filters/LoggingMDCFilter.scala#L14) in your filter chain.

More information on Logback's MDC can be found here:&nbsp;&nbsp;http://Logback.qos.ch/manual/mdc.html

Place the `LoggingMDCFilter` before any other filters which will add MDC entries or expect MDC entries to be present.

See [Logback's documentation](http://logback.qos.ch/manual/mdc.html) for more information the Mapped Diagnostic Context (MDC) functionality.

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/files"><span aria-hidden="true">&larr;</span>&nbsp;Working&nbsp;with&nbsp;Files</a></li>
    <li class="next"><a href="/finatra/user-guide/testing">Testing&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
