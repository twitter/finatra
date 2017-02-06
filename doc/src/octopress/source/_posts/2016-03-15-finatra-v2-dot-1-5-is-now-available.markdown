---
layout: post
title: "Finatra release v2.1.5 is now available!"
date: 2016-03-15 09:41:02 -0700
comments: false
categories: ["blog", "releases"]
author: cacoco
---

###  Finatra [Release 2.1.5](https://github.com/twitter/finatra/releases/tag/v2.1.5) is now available from [Maven Central][maven-central].

Please check out the [v2.1.5](https://github.com/twitter/finatra/releases/tag/v2.1.5) release tag on Github for more information.

In addition to lots of code improvements, we've addressed an issue in building our `tests` jars with sbt that should result in smaller (and better) tests jars that *only* include *actual* test utilities (and not all of our tests). Also, we've switched our default branch in Github to a [`develop` branch](https://github.com/twitter/finatra/tree/develop/) to bring us in-line with the other Twitter OSS libraries (e.g., [scrooge](https://github.com/twitter/scrooge), [finagle](https://github.com/twitter/finagle), [twitter-server](https://github.com/twitter/twitter-server)). And lastly, if you want to contribute we've added a [pull request template](https://github.com/twitter/finatra/blob/develop/.github/PULL_REQUEST_TEMPLATE.md) that should make it easy to craft a good pull request. See the [Github documentation](https://github.com/blog/2111-issue-and-pull-request-templates) for more information on pull request templates.

As always, you can download the release from [Maven Central][maven-central].

See the Finatra [Github page](https://github.com/twitter/finatra) or check out our [User's Guide][user-guide] for more information on getting started.

-- The [Finatra](https://groups.google.com/forum/#!forum/finatra-users) Team

[maven-central]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter.finatra%22%20AND%20v%3A%222.1.5%22
[user-guide]: /finatra/user-guide/index.html
