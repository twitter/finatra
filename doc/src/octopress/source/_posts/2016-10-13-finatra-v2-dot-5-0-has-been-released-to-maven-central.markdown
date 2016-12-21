---
layout: post
title: "Finatra 2.5.0 has been released to Maven Central"
date: 2016-10-13 09:36:43 -0700
comments: false
categories: ["blog", "releases"]
author: cacoco
---

###  Finatra [release 2.5.0](https://github.com/twitter/finatra/releases/tag/finatra-2.5.0) is now available from [Maven Central][maven-central].

Some of the changes in the latest releases:

- We've [*mostly* removed](https://github.com/twitter/finatra/commit/73158b8295e861390123e2abf373aea251f3841c) the need of the `maven.twttr.com` sbt resolver. It's still necessary to resolve specific Twitter thrift dependencies.
- We [fixed](https://github.com/twitter/finatra/commit/152d43dd56e88b37770e0271cbc057203b7106df) an [issue with missing files](https://github.com/twitter/finatra/issues/357) in the finatra-jackson test jar. Classes in the test `c.t.finatra.validation` package were not properly marked for 
  inclusion.
- SLFJ4 bridge installation has [changed](https://github.com/twitter/finatra/commit/807d22f5783b057341d4149b305d533d5bd4b4ec) to occur earlier and capture more of the bridged logging.
- Simplified ExceptionMapper usage. Please read the [commit notes](https://github.com/twitter/finatra/commit/36afd2c4f0dfa29108e336bdd60a534a414a9a28) as this is a breaking change.
- We've [added](https://github.com/twitter/finatra/commit/1ef8c997fd43c3bbf8220dfc94dd64e4674d22b6) support for "dark traffic filtering" in finatra-http.

Please checkout the [finatra-2.5.0](https://github.com/twitter/finatra/releases/tag/finatra-2.5.0) release notes on github for more information.

See the Finatra [Github page](https://github.com/twitter/finatra) or checkout our [User Guide](/finatra/user-guide) for more information on getting started.

-- The Finatra Team | [forum](https://groups.google.com/forum/#!forum/finatra-users) | [@finatra](https://twitter.com/finatra) | [chat](https://gitter.im/twitter/finatra)

[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20(a%3A%22finatra-http_2.11%22%20OR%20a%3A%22finatra-thrift_2.11%22)%20AND%20v%3A%222.5.0%22