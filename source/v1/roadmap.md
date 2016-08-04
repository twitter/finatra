---
title: Roadmap - Finatra
layout: finatrav1
---
<div class="page-header">
<h5><font color="red">Note:</font>&nbsp;this version is deprecated, please see the documentation for version <a href="/finatra">2.x</a>.</h5>
<h1>Roadmap</h1>
</div>
## 1.5.2

  * [refactored testing framework to allow each test to instantiate it's own controller](https://github.com/twitter/finatra/pull/70) ✗
  * [flight/bower and bootstrap built in](https://github.com/twitter/finatra/issues/63) ✗

## 1.7.x

  * [Add Aurora/Mesos support](https://github.com/twitter/finatra/issues/94) ✗
  * [asset pipeline filter](https://github.com/twitter/finatra/issues/62) ✗
  * [apache-like directory browser for files](https://github.com/twitter/finatra/issues/54) ✗
  * [benchmark suite with caliper](https://github.com/twitter/finatra/issues/45) ✗
  * [RequestAdapter does not support multiple values for query params](https://github.com/twitter/finatra/issues/22) ✗

## 1.6.x

  * [Simplify Cookie API with a CookieBuilder](https://github.com/twitter/finatra/issues/93) ✗
  * [CSRF Support](https://github.com/twitter/finatra/issues/89) ✗
  * [Session support](https://github.com/twitter/finatra/issues/88) ✗
  * [Configurable Key/Value store](https://github.com/twitter/finatra/issues/87) ✗
  * [implement a routes.txt in admin](https://github.com/twitter/finatra/issues/80) ✗
  * [support ETAGS and/or Cache-Control headers in file server](https://github.com/twitter/finatra/issues/73) ✗

## 1.5.1

  * [Fix unicode rendering in json. Correct size of response is now set](https://github.com/twitter/finatra/pull/97) ✓
  * [Stats broken after twitter-server upgrade](https://github.com/twitter/finatra/issues/95) ✓
  * [enable HTML escaping in mustache templates](https://github.com/twitter/finatra/pull/92) ✓
  * [Response tied to originating request](https://github.com/twitter/finatra/issues/86) ✓
  * [Test/Harden logging](https://github.com/twitter/finatra/issues/84) ✓
  * [LogLevel doesn't seem to work](https://github.com/twitter/finatra/issues/83) ✓
  * [enable full admin endpoints besides metrics.json](https://github.com/twitter/finatra/issues/74) ✓
  * [request.routeParams should be decoded](https://github.com/twitter/finatra/issues/68) ✓

## 1.5.0

  * [maven => sbt](https://github.com/twitter/finatra/issues/78) ✓
  * [support in release scripts for dual publishing scala 2.9 and 2.10](https://github.com/twitter/finatra/issues/75) ✓
  * [PUT and PATCH command param issue](https://github.com/twitter/finatra/issues/71) ✓

## 1.4.1

  * [Adding lazy service](https://github.com/twitter/finatra/pull/67) ✓
  * [Fixed a bug with Inheritance using Mustache](https://github.com/twitter/finatra/pull/64) ✓
