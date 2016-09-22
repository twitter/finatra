---
layout: post
title: "Announcing the Release of Finatra 2.4.0!"
date: 2016-09-07 20:02:11 -0700
comments: false
categories: ["blog", "releases"]
author: cacoco
---

###  Finatra [release 2.4.0](https://github.com/twitter/finatra/releases/tag/finatra-2.4.0) is now available from [Maven Central][maven-central].

We discovered a bug in [release 2.3.0](https://github.com/twitter/finatra/releases/tag/finatra-2.3.0) and thus it is recommended that you use [release 2.4.0](https://github.com/twitter/finatra/releases/tag/finatra-2.4.0).

Some of the changes in the latest releases:

- [Enhanced support](https://github.com/twitter/finatra/commit/b67bd039129feefcaaed68af0bbf4653331f40ad) for Java Thrift services.
- Moved all [`finatra-http`](https://github.com/twitter/finatra/tree/develop/http/src/test/scala/com/twitter/finatra/http/tests) integration tests to a package under `com.twitter.finatra.http`.
- Added a [basic file-serving example](https://github.com/twitter/finatra/tree/master/examples/web-dashboard).
- Added a Finatra [thrift implementation](https://github.com/twitter/finatra/blob/master/thrift/src/main/scala/com/twitter/finatra/thrift/filters/DarkTrafficFilter.scala) of the Finagle [AbstractDarkTrafficFilter](https://github.com/twitter/finagle/blob/develop/finagle-exp/src/main/scala/com/twitter/finagle/exp/AbstractDarkTrafficFilter.scala) which sub-classes ThriftFilter and will work in the Finatra filter chain. This will allow users to play incoming requests to a configured "dark" thrift 	service.
- Allow for the ability to disable test logging by setting the `com.twitter.inject.test.logging.disabled=true` System property.

Please checkout the [finatra-2.4.0](https://github.com/twitter/finatra/releases/tag/finatra-2.4.0) and [finatra-2.3.0](https://github.com/twitter/finatra/releases/tag/finatra-2.3.0) release notes on github for more information.

See the Finatra [Github page](https://github.com/twitter/finatra) or checkout our [User Guide](/finatra/user-guide) for more information on getting started.

-- The Finatra Team | [forum](https://groups.google.com/forum/#!forum/finatra-users) | [@finatra](https://twitter.com/finatra) | [chat](https://gitter.im/twitter/finatra)

[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20(a%3A%22finatra-http_2.11%22%20OR%20a%3A%22finatra-thrift_2.11%22)%20AND%20v%3A%222.4.0%22