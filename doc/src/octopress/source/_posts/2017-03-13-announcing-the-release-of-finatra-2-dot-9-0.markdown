---
layout: post
title: "Announcing the release of Finatra 2.9.0"
date: 2017-03-13 11:46:08 -0700
comments: false
categories: ["blog", "releases"]
author: cacoco
---

## Finatra [release 2.9.0](https://github.com/twitter/finatra/releases/tag/finatra-2.9.0) is available from [Maven Central][maven-central].

We **highly recommend** looking through the [CHANGELOG][changelog] for a full rundown of all the changes and fixes.

## Major Changes

***

### Scala 2.12 Support

We are now cross-publishing Finatra for both [Scala 2.11.8 and Scala 2.12.1](https://github.com/twitter/finatra/blob/master/build.sbt#L13). A big thanks to [__@jcrossley__](https://github.com/jcrossley)!

### Move from [grizzled-slf4j][grizzled-slf4j] to [util-slf4j-api][util-slf4j-api]

We are beginning the process of standardizing the Twitter OSS libraries on the [slf4j-api](https://www.slf4j.org/) through the functionality provided by the [util-slf4j-api][util-slf4j-api] library.

This change should be transparent to end users.

### Improved `bind[T]` testing DSL

We introduced an alternative to using the `@Bind` binding annotation for binding fields in an integration test (which we call the `bind[T]` DSL) which is exposed on all the testing utilities: 

- [`c.t.inject.app.TestInjector`](https://github.com/twitter/finatra/blob/master/inject/inject-app/src/test/scala/com/twitter/inject/app/TestInjector.scala)
- [`c.t.inject.app.EmbeddedApp`](https://github.com/twitter/finatra/blob/master/inject/inject-app/src/test/scala/com/twitter/inject/app/EmbeddedApp.scala)
- [`c.t.inject.server.EmbeddedTwitterServer`](https://github.com/twitter/finatra/blob/master/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala)
- [`c.t.finatra.http.EmbeddedHttpServer`](https://github.com/twitter/finatra/blob/master/http/src/test/scala/com/twitter/finatra/http/EmbeddedHttpServer.scala)
- [`c.t.finatra.thrift.EmbeddedThriftServer`](https://github.com/twitter/finatra/blob/master/thrift/src/test/scala/com/twitter/finatra/thrift/EmbeddedThriftServer.scala)

While useful, using `@Bind` during testing for overriding bound instances in the object graph has several practical drawbacks that we wanted to address. Specifically, two main issues. 

First, any field that accesses the embedded server or application needs to be lazy (or defined within a test method) such that the `@Bind` mechanism has time to replace the corresponding type in the object graph on server startup. Eager access to the embedded server or application negates the ability of `@Bind` to function. 

Second, the type being bound with `@Bind` needs to *exactly* match a type in the object graph in order to replace it (otherwise you would simply be binding a new type). While a simple thing to solve, (by specifying the type on a val) it nonetheless contributes to incorrect usage. The `bind[T]` DSL is more explicit in this regards.

In this release, we've updated the `bind[T]` DSL to allow binding [higher-kinded](http://blogs.atlassian.com/2013/09/scala-types-of-a-higher-kind/) types which should remove any roadblocks for its wider adoption. Using `@Bind` should now be considered deprecated.

#### <font color='red'><strong>Important</strong></font> (or why do my tests seem broken)

To support the `bind[T]` DSL in the [`c.t.inject.app.TestInjector`](https://github.com/twitter/finatra/blob/master/inject/inject-app/src/test/scala/com/twitter/inject/app/TestInjector.scala) we have updated the lifecycle of the TestInjector. Previously, a `c.google.inject.Injector` was returned directly from the `TestInjector#apply` method. The concrete consequence of the change is that you must now call `TestInjector#create` to return a `c.google.inject.Injector`. 

Without adding this your tests which use a `TestInjector` may appear to be broken.

***

See the Finatra [Github page](https://github.com/twitter/finatra) or checkout our [User's Guide][user-guide] for more information on getting started.

-- The Finatra Team | [forum](https://groups.google.com/forum/#!forum/finatra-users) | [@finatra](https://twitter.com/finatra) | [chat](https://gitter.im/twitter/finatra)

[maven-central]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20(a%3A%22finatra-http_2.12%22%20OR%20a%3A%22finatra-thrift_2.12%22)%20AND%20v%3A%222.9.0%22
[changelog]: https://github.com/twitter/finatra/blob/master/CHANGELOG.md#finatra-290-2017-03-10
[user-guide]: /finatra/user-guide/index.html
[grizzled-slf4j]: https://github.com/bmc/grizzled-slf4j
[util-slf4j-api]: https://github.com/twitter/util/blob/master/util-slf4j-api/README.md