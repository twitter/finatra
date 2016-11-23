---
layout: post
title: "Finatra 1.5.2 Released"
date: 2014-02-02 06:11:00
comments: true
categories: ["blog", "releases"]
author: capotej
---

### Bootstrap and Bower built in
<p class="lead">
Finatra now generates a [Bower](http://bower.io/) compatible app with
[Bootstrap](http://getbootstrap.com) out of the box:

![Bower Screenshot](/finatra/images/bower-screenshot.png)

### New Testing System
<p class="lead">
Write tests for just a Controller instead of an entire app:

{% highlight scala %}
class SampleController extends Controller {
  get("/testing") {
    request => render.plain("hi").toFuture
  }
}

"GET /testing" should "be 200" in {
  val app = MockApp(new SampleController)
  val response = app.get("/testing")

  response.code should be(200)
  response.body should be("hi")
}
{% endhighlight %}

<p class="lead">
See the [Pull Request](https://github.com/twitter/finatra/pull/70) for more details.

<br/>

### Changelog
<p class="lead">
[f5c52b](https://github.com/twitter/finatra/commit/f5c52b28d9d6ca20b26b6dac5a964ae90cda45a0) Fixed bug in multipart form uploads that contain both text and file fields.

<p class="lead">
[cbb32a](https://github.com/twitter/finatra/commit/cbb32ac53b472d5866bdde9c9acef24ad8a7c1f9), [261627](https://github.com/twitter/finatra/commit/2616275dd970a6ce5b018598fd8bd5a13d6069fb) Update mustache.java to 0.8.14.

<p class="lead">
[43b244](https://github.com/twitter/finatra/commit/43b244aeca43a0f71ada196bdb21a06e56037622) Set Content-Length on static file responses.

<p class="lead">
[9c64c6](https://github.com/twitter/finatra/commit/9c64c63519058cd40b6100ac86e35c828ec3801c) New test framework with MockApp.

<p class="lead">
[511b4f](https://github.com/twitter/finatra/commit/511b4fceaf75bb0c23523e5d97a494330a88ae07) Generate bootstrap and bower config files for new apps.

<p class="lead">
[807859](https://github.com/twitter/finatra/commit/807859b4181244c2dc365493866adad4284ea5c9) Update twitter-server to 1.4.1 and finagle-stats to 6.11.1.

<br/>

#### SBT

```scala
"com.twitter" %% "finatra" % "1.5.2"
```

#### Maven

```xml
<dependency>
  <groupId>com.twitter</groupId>
  <artifactId>finatra_2.10</artifactId>
  <version>1.5.2</version>
</dependency>
```
