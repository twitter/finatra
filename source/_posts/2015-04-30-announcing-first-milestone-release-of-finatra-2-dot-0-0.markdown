---
layout: post
title: "Announcing first milestone release of Finatra 2.0.0!"
date: 2015-04-30 14:05:13 -0700
comments: false
categories: ["blog", "milestone", "releases"]
author: cacoco
---

#### We're pleased to announce the first milestone release of Finatra 2.0.0!

You can download the release from [Maven Central][maven-central].

#### Features:

* Significant performance improvements over v1.6.0.
* Powerful feature and integration test support.
* JSR-330 Dependency Injection using [Google Guice][guice].
* [Jackson][jackson] based JSON parsing with additional support for required fields, default values, and custom validations.
* [Logback][logback] [MDC][mdc] integration with [com.twitter.util.Local][local] for contextual logging across futures.

See the Finatra [github page](https://github.com/twitter/finatra) for more information on getting started.

-- The [Finatra](https://groups.google.com/forum/#!forum/finatra-users) Team

[guice]: https://github.com/google/guice
[jackson]: https://github.com/FasterXML/jackson
[logback]: http://logback.qos.ch/
[local]: https://github.com/twitter/util/blob/master/util-core/src/main/scala/com/twitter/util/Local.scala
[mdc]: http://logback.qos.ch/manual/mdc.html
[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter.finatra%22
