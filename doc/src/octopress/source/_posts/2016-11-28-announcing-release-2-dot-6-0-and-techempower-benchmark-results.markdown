---
layout: post
title: "Announcing Finatra release 2.6.0 and TechEmpower Round 13 Results!"
date: 2016-11-28 09:42:25 -0800
comments: false
categories: ["blog", "releases"]
author: cacoco
---

###  Finatra [release 2.6.0](https://github.com/twitter/finatra/releases/tag/version-2.6.0) is now available from [Maven Central][maven-central].

Some changes from the latest release (there are *several* dependency updates as we start to prepare for supporting a Scala 2.12 release):

- We've [upgraded to Jackson 2.8.3](https://github.com/twitter/finatra/commit/e8e7837a9c79f67dac40ce21ef361104edcf99d7) (from Jackson 2.6.5) and then to [Jackson 2.8.4](https://github.com/twitter/finatra/commit/c88584586eeb1350b84e471f322355f1c97bfd7e)!
- We've [upgraded to Scalatest 3.0.0 and Scalacheck 1.13.4](https://github.com/twitter/finatra/commit/c070bcfc4617332eb38bc0ce65f5744fb6670c92). 
- [This site](http://twitter.github.io/finatra/)'s source is no longer maitained in the `gh-pages-source` branch in Github but is now [co-located with the code source](https://github.com/twitter/finatra/commit/0b40f562d5ca5bb2010581b398aaab9daf3755c5).
- Support for ["extended boolean"](https://github.com/twitter/finatra/commit/5e6c852e44e680e5cd240432c5179ae2eb06e1f9) `@QueryParam` annotated case class fields (e.g., support for "t", "f", "1", "0" parsed as booleans).
- [Dropped the need to add `@Inject`](https://github.com/twitter/finatra/commit/5446b62909472fd348d9e8ffc098c1343ab3c559) on the finagle-http `Request` field in a custom case class request. The finagle-http `Request` was not actually "injected" and use of the `@Inject` annotation as a marker caused undue confusion. See the documentation [here](http://twitter.github.io/finatra/user-guide/build-new-http-server/controller.html#requests) for more information.

**Note:** Please update your dependencies accordingly.

Please checkout the [version-2.6.0](https://github.com/twitter/finatra/releases/tag/version-2.6.0) release notes on github for more information.

TechEmpower Benchmark Results: 3rd fastest Scala Framework!
=================================================================

Finatra has been added to the [Techempower Web Famework Benchmarks](https://www.techempower.com/benchmarks/) and in the lastest Round 13 benchmarking came out as the [3rd fastest Scala Framework for JSON serialization](https://www.techempower.com/benchmarks/#section=data-r13&hw=ph&test=json&l=4ftbsv).

### JSON Serialization results

Scala Frameworks: 3/14

https://www.techempower.com/benchmarks/#section=data-r13&hw=ph&test=json&l=4ftbsv

JVM Frameworks: Scala, Java, Groovy, Clojure: 12/55

https://www.techempower.com/benchmarks/#section=data-r13&hw=ph&test=json&l=4ftbob

For more information on Round 13 of the [TechEmpower Web Famework Benchmarks](https://www.techempower.com/benchmarks/) see the blog post [here](https://www.techempower.com/blog/2016/11/16/framework-benchmarks-round-13/).

[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20(a%3A%22finatra-http_2.11%22%20OR%20a%3A%22finatra-thrift_2.11%22)%20AND%20v%3A%222.6.0%22
