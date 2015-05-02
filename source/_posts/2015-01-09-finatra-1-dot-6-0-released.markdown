---
layout: post
title: "Finatra 1.6.0 Released"
date: 2015-01-09 11:00:00
comments: false
categories: ["blog", "releases"]
author: capotej
---

### Last release for 1.x.x and Scala 2.9
<p class="lead">
As we prepare for the upcoming 2.x.x release, this will be the last 1.x.x release
and also the last release to be published for scala 2.9.x.

### New Finatra and Maintainer!
<p class="lead">
I'm happy to announce that [Steve Cosenza](https://twitter.com/scosenza) will be
taking over the project for the 2.x.x release and beyond. We're excited to release
the next major iteration of Finatra and share everything we've learned using it in
production at [Twitter](https://twitter.com) over the course of a year.

<br/>

### Changelog
<p class="lead">
[d90c77](https://github.com/twitter/finatra/commit/d90c77dbd09fb74abacb437258f1d60bf92efd8d) Can use ChannelBuffer's as Response

<p class="lead">
[d73cbb](https://github.com/twitter/finatra/commit/d73cbbf644f58ed1dd756f186b1e272e31b0ae39), [819271](https://github.com/twitter/finatra/commit/8192716f00ae513358d1210645e4e47352be8e48) Fix controller-specific ErrorHandler's

<p class="lead">
[129184](https://github.com/twitter/finatra/commit/129184a90f4255a97e2983407b324747f8c218e4) Fixed Heroku Deployments

<p class="lead">
[04b5cb](https://github.com/bpfoster/finatra/commit/04b5cbb9e71488d590eab76cffcc284685cbaa47) 2 Way SSL support

<p class="lead">
[fcce16](https://github.com/twitter/finatra/commit/fcce1669c3b2e5a14d7a242db64b226f8be3b73b) Quieter errors in the logs

<p class="lead">
[33b106](https://github.com/twitter/finatra/commit/33b1063d4c59d8ea82dc37f176484cde64f26bc8) Asset Directory browser! set com.twitter.finatra.showDirectories=true to enable

<p class="lead">
[2edd33](https://github.com/twitter/finatra/commit/2edd33df0bd5a5be4d6161b7c8bd53954593939f) Clean up build files, lock to twitter-server 1.7.3

<p class="lead">
[982703](https://github.com/twitter/finatra/commit/982703ab79900d6a0e2ab7d6ff039accf0723979) Remove scalatest dependency from jar, must now depend on it yourself

<br/>

#### SBT

```scala
"com.twitter" %% "finatra" % "1.6.0"
```

#### Maven

```xml
<dependency>
  <groupId>com.twitter</groupId>
  <artifactId>finatra_2.10</artifactId>
  <version>1.6.0</version>
</dependency>
```
