---
layout: post
title: "Finatra release 2.8.0 is out and available!"
date: 2017-02-06 08:46:55 -0800
comments: false
categories: ["blog", "releases"]
author: cacoco
---

## Finatra [release 2.8.0](https://github.com/twitter/finatra/releases/tag/finatra-2.8.0) is available from [Maven Central][maven-central].

There are a lot of updates in this release.

We **highly recommend** looking through the [CHANGELOG][changelog] for a full rundown of all the changes and fixes.

## Major Changes

***

### [ScalaTest][scalatest] Testing Utilities

One of the largest impacting changes is the introduction of [Test Mixins][test-mixins] helpers and the long-time coming move of Finatra's opinionated [ScalaTest][scalatest] testing style from [WordSpec][wordspec-glance] to [FunSuite][funsuite-glance].

It is known that Twitter engineering transitioned from a mostly Rails codebase to Scala. In this transition, we originally used [Scala Specs](https://github.com/etorreborre/specs) for most Scala projects. As we evolved the Scala codebase we moved to [ScalaTest][scalatest] and a lot of code at the time adopted the [WordSpec][wordspec] [ScalaTest][scalatest] testing style to easily [convert from Specs to ScalaTest][wordspec-glance]. 

It was at this time that we started version 2 of the Finatra framework and thus coupled (too tightly) the Finatra testing tooling to [WordSpec][wordspec]. A short time thereafter, however, Twitter engineering settled on a recommendation of [FunSuite][funsuite]. Unfortunately, Finatra did not provide a way for users to use this [ScalaTest][scalatest] testing style.

#### Testing Changes

In [this release](https://github.com/twitter/finatra/blob/master/CHANGELOG.md#finatra-280-2017-02-03) we have introduced a set of [Test Mixins][test-mixins] which allow users to use the Finatra test tooling and bring their own [ScalaTest][scalatest] testing style. Since the original framework test tooling was specific to [WordSpec][wordspec] you will find that functionality has now been prefixed with `WordSpec`. E.g., if you were using

`Test` it is now [`WordSpecTest`](https://github.com/twitter/finatra/blob/master/inject/inject-core/src/test/scala/com/twitter/inject/WordSpecTest.scala),  
`IntegrationTest` is now [`WordSpecIntegrationTest`](https://github.com/twitter/finatra/blob/master/inject/inject-core/src/test/scala/com/twitter/inject/WordSpecIntegrationTest.scala), and  
`FeatureTest` is now [`WordSpecFeatureTest`](https://github.com/twitter/finatra/blob/master/inject/inject-server/src/test/scala/com/twitter/inject/server/WordSpecFeatureTest.scala).

You'll notice that these [WordSpec][wordspec] versions are deprecated since they will be removed at a point in the future. This is because &mdash; as mentioned above &mdash; the Twitter engineering recommendation is to use [FunSuite][funsuite] (see [FunSuite at a glance](http://www.scalatest.org/at_a_glance/FunSuite)) and we have introduced opinionated versions of the testing tools that mix in [FunSuite][funsuite]:

[`c.t.inject.Test`](https://github.com/twitter/finatra/blob/master/inject/inject-core/src/test/scala/com/twitter/inject/Test.scala)  
[`c.t.inject.IntegrationTest`](https://github.com/twitter/finatra/blob/master/inject/inject-core/src/test/scala/com/twitter/inject/IntegrationTest.scala)  
[`c.t.inject.server.FeatureTest`](https://github.com/twitter/finatra/blob/master/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala)

#### <font color='red'><strong>Important</strong></font> (or why do my tests seem broken)

Therefore, when you update to [release 2.8.0](https://github.com/twitter/finatra/releases/tag/finatra-2.8.0) it *may look like your tests are broken* as they would have been written against [WordSpec][wordspec] but the tools you are using are now written against [FunSuite][funsuite]. To remedy this situation, simply update to use the `WordSpec` prefixed versions instead as these are completely backwards compatible. Even better would be to update your tests to [FunSuite][funsuite] or create your own [WordSpec][wordspec] testing versions based on the [Test Mixins][test-mixins].

#### Naming

Why didn't we prefix the new versions of the utilities with `FunSuite`, e.g, `FunSuiteTest`, `FunSuiteIntegrationTest`, and `FunSuiteFeatureTest`? 

The answer is two-fold.  
1. You don't have to use them. You can always choose to use the [Test Mixins][test-mixins] directly.  
2. We want the framework to express an opinion about the [ScalaTest][scalatest] testing style to use and thus prefixing them would potentially connote that we plan to support multiple ScalaTest testing styles simultaneously. Which is not true.

### Admin Paths and Routing Changes

Another significant change is the new ability to expose paths that begin with `/admin` on your external interface. Finatra now allows for more flexibility in your admin routing which also includes the ability to add routes which **do not begin** with `/admin` to the admin interface.

Please see the section on [Admin Paths](/finatra/user-guide/http/controllers.html#admin-paths) in the [User's Guide][user-guide] or take a look at the [`c.t.finatra.http.internal.routing.AdminHttpRouter`](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/internal/routing/AdminHttpRouter.scal) for more important information on how this works.

### Changes to Utilities

We have finally addressed some long-standing issues around the organization of the codebase. 

For more context, Finatra is divided into three main parts: `inject`, `http`, and `thrift`. The `inject` part of the framework contains all of the base functionality for integration of the Guice dependency injection framework with [TwitterUtil](https://twitter.github.io/util/)'s [`c.t.app.App`](https://twitter.github.io/util/docs/com/twitter/app/App.html) and [TwitterServer](https://twitter.github.io/twitter-server/)'s `c.t.server.TwitterServer` (which is just an extension of `c.t.app.App`). The `inject` layer is for concerns that span both http and thrift services.

Modules in the `inject` part of the Finatra framework constitute the core of the framework, are in the `com.twitter.inject` package namespace, and as such only depend on each other or outside dependencies (except in testing). Everything else in the Finatra codebase is in the `com.twitter.finatra` package namespace and has dependencies on the `inject` libraries as well as other libraries. A long-term plan is to separate `inject` into its own project but we are not quite there yet. However, the changes in this release are a step in that direction.

#### <font color='red'><strong>Important</strong></font> (or where has `WrappedValue` gone)

Unfortunately, there are things that were added to the `com.twitter.finatra` package namespace that make more sense in the core of the framework and have now been [moved from `com.twitter.finatra` to the `com.twitter.inject` package namespace](https://github.com/twitter/finatra/commit/c1d476859e9cac23679e0afe13cfbcf83f90758c). These were moved from `finatra-utils` to `inject-utils` and include:

- all conversions utilities, e.g., everything previously in `c.t.finatra.conversions` is now `c.t.inject.conversions`.
- `c.t.finatra.domain.WrappedValue` is now `c.t.inject.domain.WrappedValue`
- `FutureUtils`, `RetryPolicyUtils`, and `RetryUtils` are now in the `c.t.inject.utils` package namespace.

We apologize for the churn this may cause but feel it is for the betterment of the framework. Ideally, you need only do a "search-and-replace" to update package import statements.

## Other Changes

***

As mentioned, there are lot of [changes and fixes][changelog] in this release. Here's a quick rundown (for more information see the [CHANGELOG][changelog]):

- Updated [User's Guide][user-guide]. We've added more documentation and updated the layout. Please take a look!
- Introduced support for JSON Patch. See the [JSON Patch](/finatra/user-guide/http/requests.html#json-patch-requests) section in the [User's Guide][user-guide] for more information.
- Introduced a simple DSL for easily [defining a route prefix](/finatra/user-guide/http/controllers.html#route-prefixes) for a set of Controller routes.
- Support for matching [optional trailing slashes](/finatra/user-guide/http/controllers.html#trailing-slashes) in Controller routes.
- More HTTP server Java support: including [allowing the specification of admin routes](https://github.com/twitter/finatra/commit/8671f7969ccfa3aaea90f0565f0288726dbefad7) and [better support for registering exception mappers](https://github.com/twitter/finatra/commit/0d4e7ac92d5d7fad9ff3b1175efb581eba898535).
- [Fixed](https://github.com/twitter/finatra/commit/34937784a1be0997671ffedc121301c1d3762382) Github [issue #373](https://github.com/twitter/finatra/issues/373) around the TestInjector not properly reading the default for boolean flags. 
- [Fixed](https://github.com/twitter/finatra/commit/b55b702bd3d20b570d798726c9259788eee973e9) an IllegalArgumentException in `FinatraCaseClassDeserializer`. Thanks to [__@AlexITC__](https://github.com/AlexITC) for the contribution. 
- [Fixed](https://github.com/twitter/finatra/commit/e226beee463bf2ef620eb3792230f4c5d245fd41) issue around "injectable value" fields of a collection type e.g., a custom case class request object field annotated with `@RouteParam`, `@QueryParam`, or `@FormParam` of a collection type, not correctly picking up default values.
- A [fix](https://github.com/twitter/finatra/commit/8f4820c2b20f82d64f86f898d10a73f4ac1e123e) to correctly publish sources and javdoc jar for published Finatra test-jars.

***

See the Finatra [Github page](https://github.com/twitter/finatra) or checkout our [User's Guide][user-guide] for more information on getting started.

-- The Finatra Team | [forum](https://groups.google.com/forum/#!forum/finatra-users) | [@finatra](https://twitter.com/finatra) | [chat](https://gitter.im/twitter/finatra)

[maven-central]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20(a%3A%22finatra-http_2.11%22%20OR%20a%3A%22finatra-thrift_2.11%22)%20AND%20v%3A%222.8.0%22
[scalatest]: http://www.scalatest.org/
[wordspec]: http://doc.scalatest.org/3.0.0/#org.scalatest.WordSpec
[wordspece-glance]: http://www.scalatest.org/at_a_glance/WordSpec
[funsuite]: http://doc.scalatest.org/3.0.0/#org.scalatest.FunSuite
[funsuite-glance]: http://www.scalatest.org/getting_started_with_fun_suite
[test-mixins]: /finatra/user-guide/testing/index.html#test-mixins
[changelog]: https://github.com/twitter/finatra/blob/master/CHANGELOG.md#finatra-280-2017-02-03
[user-guide]: /finatra/user-guide/index.html

