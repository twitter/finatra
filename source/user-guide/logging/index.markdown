---
layout: page
title: "Logging"
comments: false
sharing: false
footer: true
---

Finatra uses [SLF4J](http://www.slf4j.org/manual.html) for framework logging. By coupling the framework to only the SLF4J-api, application developers are free to choose their logging implementation.

##### From the [SLF4J](http://www.slf4j.org/manual.html) documentation:
>"The Simple Logging Facade for Java serves as a simple facade or abstraction for various logging frameworks, such as java.util.logging, Logback and log4j. SLF4J allows the end-user to plug in the desired logging framework at deployment time."

Adding [finatra/slf4j](https://github.com/twitter/finatra/tree/master/slf4j) as a dependency adds an SLF4J bridge for the following logging providers:

  * [Log4j](http://en.wikipedia.org/wiki/Log4j)
  * [commons-logging](http://commons.apache.org/proper/commons-logging/)
  * jdk14-logging
  * [java.util.logging](http://docs.oracle.com/javase/7/docs/api/index.html?java/util/logging/package-summary.html): There is a performance penalty for intercepting jul log messages, so make sure to also include the [Slf4jBridgeModule](../slf4j/src/main/scala/com/twitter/finatra/logging/modules/Slf4jBridgeModule.scala) in your list of modules, as this will install the [SLF4JBridgeHandler](http://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html) which mitigates most of the performance penalty. e.g.,

  ```scala
  class Server extends HttpServer {
    override val modules = Seq(
      Slf4jBridgeModule,
      ...)

    ...
  }
  ```

## Basics

Since SLF4J is an interface, it requires an actual logging implementation. Howver, you should ensure that you do not end-up with *multiple* logging implementations on your classpath, e.g., you should not have multiple SLF4J bindings (slf4j-nop, slf4j-log4j12, slf4j-jdk14, etc.) and/or a java.util.logging implementation, etc. on your classpath as these are all competing implementations and since classpath order is non-deterministic this will lead to unexpected logging behavior.

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

### Logback
===============================

We highly recommend using [Logback](http://logback.qos.ch/) as an SLF4J binding (logging implementation). If you choose to use Logback, just include jar dependencies on `ch.qos.logback:logback-classic` in addition to `com.twitter.finatra:finatra-slf4j`. This will provide a Logback SLF4J implementation (`logback-classic`) in addition to the logging bridges provided by finatra/slf4j.

### Configuration
===============================

See [logback.xml](../examples/hello-world/src/main/resources/logback.xml) and [logback-test.xml](../examples/hello-world/src/test/resources/logback-test.xml) in the [hello-world](../examples/hello-world) example project.

### [MDC](http://logback.qos.ch/manual/mdc.html) Filters
===============================

Place the [LoggingMDCFilter](../logback/src/main/scala/com/twitter/finatra/logging/filter/LoggingMDCFilter.scala) filter before any other filters which will add entries or expect MDC entries to be present.

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/build-new-http-server"><span aria-hidden="true">&larr;</span>&nbsp;Building&nbsp;a&nbsp;new&nbsp;HTTP&nbsp;Server</a></li>
    <li class="next"><a href="/finatra/user-guide/testing">Testing&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
