---
layout: post
title: "Finatra 1.5.0 Released"
date: 2014-01-06 16:29:18
comments: false
categories: ["blog", "releases"]
author: capotej
---

<p class="lead">This is the first release to be dual published for scala 2.9.x and 2.10.x, as such the `artifactId` has changed from finatra to `finatra_2.9.2` or `finatra_2.10`, respectively.

#### SBT

```scala
"com.twitter" %% "finatra" % "1.5.0"
```

#### Maven

```xml
<dependency>
  <groupId>com.twitter</groupId>
  <artifactId>finatra_2.10</artifactId>
  <version>1.5.0</version>
</dependency>
```
### Download
[https://github.com/twitter/finatra/archive/1.5.0a.zip](https://github.com/twitter/finatra/archive/1.5.0a.zip)

<br>

## Header API change
<p class="lead">
There is a breaking change in the way `request.headers` are accessed: it now returns an [HttpHeaders](http://netty.io/3.8/api/org/jboss/netty/handler/codec/http/HttpHeaders.html) object instead of the previous [HeaderMap](https://github.com/twitter/finagle/blob/master/finagle-http/src/main/scala/com/twitter/finagle/http/HeaderMap.scala), which is now located at `request.headerMap`.

<p class="lead">
Changing all `request.headers` calls to `request.headerMap` should be sufficient. For more examples, see these commits: [6da615](https://github.com/twitter/finagle/commit/6da615a1ec2bc1dfdd8e7de0716c67590afa39d3), [1dd624](https://github.com/twitter/finatra/commit/1dd6243fbf2fea2a3a20bbe1195f78299c0bb08c) and [b95119](https://github.com/twitter/finatra/commit/b95119ab5b993caeaefed851c483660f3f3fa9fb).

<p class="lead">
**Note**: `render.headers` is not affected by this change.

<br>

## SBT Support
<p class="lead">
The [example app](http://github.com/capotej/finatra-example), [Finatra itself](https://github.com/twitter/finatra/blob/master/build.sbt), and the app generator are now all SBT compatible.

<br>

## Bugfixes
<p class="lead">
[6fc3003](https://github.com/BenWhitehead/finatra/commit/6fc3003acf4a22bb5ee82acba6ad15b57a5a9ad3) Add Content-Length to responses.

<p class="lead">
[12feaf](https://github.com/twitter/finatra/commit/12feaf27b56755446bb330ca91caf5ee0ee26fb9) Upgrade to finagle 6.10/twitter-server 1.4.0, fixing [#71](https://github.com/twitter/finatra/issues/71).
