---
layout: post
title: "Announcing the release of Finatra 2.7.0"
date: 2017-01-13 10:06:54 -0800
comments: false
categories: ["blog", "releases"]
author: cacoco
---

###  Finatra [release 2.7.0](https://github.com/twitter/finatra/releases/tag/version-2.7.0) is available from [Maven Central][maven-central].

This release was done over the holidays. A big thanks to [__@jcrossley__](https://github.com/jcrossley)!

Some of the changes in this release include:

- [Support for Scala's `scala.concurrent.Future` in the `CallbackConverter`](https://github.com/twitter/finatra/commit/f7d617163d6981d779dca66fcc67ddd33c6aa083). Now controller callbacks with a return type of `scala.concurrent.Future` will be converted via a [bijection to Twitter's `c.t.util.Future`](http://twitter.github.io/util/guide/util-cookbook/futures.html#conversions-between-twitter-s-future-and-scala-s-future). Thanks to [__@virusdave__](https://github.com/virusdave) for the contribution.

- [Fix for `ResponseBuilder.fileOrIndex`](https://github.com/twitter/finatra/commit/b46e13273e4709e598e5bea5540ff81bf6a1c733) to verify the requested file actually exists before returning the index. Thanks to [__@AlexITC__](https://github.com/AlexITC) for the contribution.

- Support for [Request forwarding with Controllers](https://github.com/twitter/finatra/commit/3ff9dbcd4fbb10b9c8769224c91267d275286ccc). Requests can be now "forwarded" from one Controller route to another. See the [user-guide](http://twitter.github.io/finatra/user-guide/build-new-http-server/controller.html#forwarding-requests) for more information.

- The [grizzled-slf4j](https://github.com/bmc/grizzled-slf4j) and [codingwell/scala-guice](https://github.com/codingwell/scala-guice) dependency versions [were updated](https://github.com/twitter/finatra/commit/ec57a4b5295148309bc6638a04707141cf26eba6).

<br/>
  
Please checkout the [Finatra 2.7.0](https://github.com/twitter/finatra/releases/tag/finatra-2.7.0) release notes on github for more information.

See the Finatra [Github page](https://github.com/twitter/finatra) or checkout our [User Guide](/finatra/user-guide) for more information on getting started.

-- The Finatra Team | [forum](https://groups.google.com/forum/#!forum/finatra-users) | [@finatra](https://twitter.com/finatra) | [chat](https://gitter.im/twitter/finatra)

[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20(a%3A%22finatra-http_2.11%22%20OR%20a%3A%22finatra-thrift_2.11%22)%20AND%20v%3A%222.7.0%22
