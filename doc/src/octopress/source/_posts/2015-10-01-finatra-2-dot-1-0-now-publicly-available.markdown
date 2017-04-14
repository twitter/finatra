---
layout: post
title: "Finatra 2.1.0 now publicly available"
date: 2015-10-01 10:50:53 -0700
comments: false
categories:  ["blog", "releases"]
author: cacoco
---

### We're very happy to announce the [release](https://github.com/twitter/finatra/releases/tag/v2.1.0)  of Finatra 2.1.0!

There are several improvements and features in this release:

- the ability to "disable" the [TwitterServer HTTP Admin Interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface) for deploying to environments that only allow binding to a *single* port for your application.
- the ability to "turn-off" automatic JSON body parsing into a case class for requests that represent JSON blobs (useful for document stores).
- Closed issues and pull-requests:

  * https://github.com/twitter/finatra/pull/228
  * https://github.com/twitter/finatra/pull/253
  * https://github.com/twitter/finatra/pull/255
  * https://github.com/twitter/finatra/issues/257
  * https://github.com/twitter/finatra/pull/258

You can download the release from [Maven Central][maven-central].

For a full list of changes and updates from the previous version, see the diff [here](https://github.com/twitter/finatra/compare/v2.0.1...v2.1.0).

See the Finatra [Github page](https://github.com/twitter/finatra) for more information on getting started.

-- The [Finatra](https://groups.google.com/forum/#!forum/finatra-users) Team

[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter.finatra%22%20AND%20v%3A%222.1.0%22
