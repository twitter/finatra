---
layout: post
title: "Announcing Finatra Release 2.2.0!"
date: 2016-06-30 08:58:38 -0700
comments: false
categories: ["blog", "releases"]
author: cacoco
---

###  Finatra [release 2.2.0](https://github.com/twitter/finatra/releases/tag/finatra-2.2.0) is now available from [Maven Central][maven-central].

This is our first release to support JDK 8 and Scala 2.11 **only**. We've also added a lot of useful [scaladocs](finatra/docs/index.html) throughout the codebase, so take a look!

Please note that the published  [groupId](https://maven.apache.org/guides/mini/guide-naming-conventions.html) <font color='red'><strong>for all Finatra artifacts has changed</strong></font> from `com.twitter.finatra` and `com.twitter.inject` to **just** `com.twitter`. You can find the published v2.2.0 artifacts [here][maven-central].

Here's a list of some of the major changes, updates, and fixes:

- The [groupId](https://maven.apache.org/guides/mini/guide-naming-conventions.html) of both finatra and finatra/inject published artifacts has changed from `com.twitter.finatra` and `com.twitter.inject` respectively to [just `com.twitter`](https://github.com/twitter/finatra/commit/7f25e3c51c368f901538e38959656fbf1cf45e76).
- [SLF4J](https://www.slf4j.org/) api has been [updated to 1.7.21](https://github.com/twitter/finatra/commit/a174f36638919887ef052bbdd3443b1c58869b43) and examples and tests using [Logback][logback] have been updated to 1.1.7. It is recommended that you also update your version if you are using the [Logback][logback] logging implementation.
- The [Jackson](https://github.com/FasterXML/jackson-module-scala) Module Scala has been updated to [version 2.6.5](https://github.com/twitter/finatra/commit/bb1288a84f8fc4d98919ceb35aab35f69200167c).
- Support for [`any` HTTP method](https://github.com/twitter/finatra/commit/7123ba1122cbb60c8be509205a95d2b587942360) in HTTP Controllers.
- HTTP test utilities have been [moved to a consistent package naming](https://github.com/twitter/finatra/commit/e61e6e309c2cc892306d0f93f9d4bc4b5605b020). Thus utilities formerly in `com.twitter.finatra.http.test` **are now packaged** `com.twitter.finatra.http`. As always these test utilities are available in the [finatra-http test jar](https://oss.sonatype.org/#nexus-search;gav~com.twitter~finatra-http_2.11~2.2.0~~tests).
- The utility method for running a "Warmup Handler" has been [renamed from](https://github.com/twitter/finatra/commit/01f20ee9145b369220f0926ecafb3de523aebec6) `com.twitter.inject.server.TwitterServer#run` to `com.twitter.inject.server.TwitterServer#handle`.
- [Fixed a bug](https://github.com/twitter/finatra/commit/b6e65310997c4ea3c6d24ac24cbf3f1c6395ce1d) in the handling of java callbacks which return a Future hidden as a `java.lang.Object` in the [`com.twitter.finatra.http.internal.marshalling.CallbackConverter`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/CallbackConverter.scala).
- Removal of deprecated [packages](https://github.com/twitter/finatra/commit/f4334a0dc265e4b0b5cbd35ccf225f1dae109888) and [code](https://github.com/twitter/finatra/commit/e143bb075a1954eef605d332af7994cdbbbc38c7).
- Simplified and brought consistency to the Embedded testing utilities [constructor arguments](https://github.com/twitter/finatra/commit/5915a9818851386f0416f27943766255f80d2912).
- [`FilteredThriftClientModule`](https://github.com/twitter/finatra/commit/a7650fefe3787fad7dc45af50a48bcd376ebc56f) and [general thrift improvements](https://github.com/twitter/finatra/commit/d0ae1d9a0e17f6b9b11edfc9765ed1f659805dc8) to better capture and stat exceptions.
- [Narrowed visibility](https://github.com/twitter/finatra/commit/a3275378ae96b1a4d3af5f6ab81115eba247a0eb) on classes and objects in **internal** [packages](https://github.com/twitter/finatra/tree/develop/http/src/main/scala/com/twitter/finatra/http/internal) which are not necessarily meant to be publicly exposed. It is expected that users depend only on the "public" framework APIs and not internal framework implementations.
- [Re-worked](https://github.com/twitter/finatra/commit/b8f9b609f595ba7e6f996fd1f4c31d6933e6eb04) the server lifecycle to address confusion around naming and intent. Added scaladocs around the lifecycle to better aid in understanding of what the framework is providing on top of TwitterServer and addressed lot of testing utility issues including re-writing EmbeddedApp to make it simpler/easier to test cmd line applications (which is the central purpose of `com.twitter.inject.app.App`).

See the Finatra [Github page](https://github.com/twitter/finatra) or checkout our [User's Guide][user-guide] for more information on getting started.

-- The Finatra Team | [forum](https://groups.google.com/forum/#!forum/finatra-users) | [@finatra](https://twitter.com/finatra) | [chat](https://gitter.im/twitter/finatra)

[maven-central]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20v%3A%222.2.0%22
[logback]: https://logback.qos.ch/
[user-guide]: /finatra/user-guide/index.html
