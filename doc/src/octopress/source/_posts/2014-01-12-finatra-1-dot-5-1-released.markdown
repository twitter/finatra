---
layout: post
title: "Finatra 1.5.1 Released"
date: 2014-01-13 16:29:18
comments: false
categories: ["blog", "releases"]
author: capotej
---

### Response internals fix
<p class="lead">
It is now possible to modify the response "out of band", since `Response` is tied to `Request` via `request.response`. For example, in a filter:

<pre class="prettyprint">
class ExampleFilter
  extends SimpleFilter[Request, Response] {

  def apply(
    request: FinagleRequest,
    service: Service[Request, Response])
  ) = {
    request.response.headers.add("X-Filtered", "True")
    service(request)
  }

}
</pre>

<p class="lead">
See [RequestResponseSpec](https://github.com/twitter/finatra/blob/1.5.1/src/test/scala/com/twitter/finatra/RequestResponseSpec.scala) for more details.



<br/>

### Changelog
<p class="lead">
[75ebe3](https://github.com/twitter/finatra/commit/75ebe3c23678bba40cff0493b7f9806cf27de883) Correctly set Content-Length for unicode JSON responses

<p class="lead">
[145981](https://github.com/twitter/finatra/commit/1459817858f6dea218ab7de4861b3bba14ccadbc) Depend on finagle-stats, fixes [#95](https://github.com/twitter/finatra/issues/95)

<p class="lead">
[9037f1](https://github.com/twitter/finatra/commit/9037f13e7fce0dec361e86eb2e0fd6b5e1a5ca74) Enable HTML escaping in mustache templates

<p class="lead">
[83fa4f](https://github.com/twitter/finatra/commit/83fa4f8d286fb21144861303ab4e79eb3250d082) Response tied to originating Request, fixes [#86](https://github.com/twitter/finatra/issues/86)

<p class="lead">
[635b4d](https://github.com/twitter/finatra/commit/635b4dcaaf91ab6d0c39c0c9efb10ab24fa8fd36) Test and fix for setting `logLevel` correctly, fixing [#83](https://github.com/twitter/finatra/issues/83) and [#84](https://github.com/twitter/finatra/issues/84)

<p class="lead">
[a95d3e](https://github.com/twitter/finatra/commit/a95d3e5c5055405958ad8295919fb828bb5748eb) Adds all Admin endpoints, fixes [#74](https://github.com/twitter/finatra/issues/74)

<p class="lead">
[d8c2be](https://github.com/twitter/finatra/commit/d8c2beb4dcc3c4a7439f70b14ca3dd923571e870) `request.routeParams` are now properly url decoded, fixing [#68](https://github.com/twitter/finatra/issues/68)

<br/>

#### SBT

```scala
"com.twitter" %% "finatra" % "1.5.1"
```

#### Maven

```xml
<dependency>
  <groupId>com.twitter</groupId>
  <artifactId>finatra_2.10</artifactId>
  <version>1.5.1</version>
</dependency>
```
