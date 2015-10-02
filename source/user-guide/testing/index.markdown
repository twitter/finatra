---
layout: user_guide
title: "Testing"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li class="active">Testing</li>
</ol>

Ease of testing is a core philosophy of the Finatra framework and Finatra provides a powerful testing framework allowing for very robust tests of different types.

## Basics

Finatra provides the following testing features:

- the ability to start a locally running service and easily issue requests against it and assertion behavior on the responses.
- the ability to easily replace or override class implementations throughout the object graph.
- the ability to retrieve classes in the object graph to perform assertions on them.
- the ability to write tests and deploy test instances without deploying test code to production.

## <a name="testing-types" href="#testing-types">Types of Tests</a>

What are we talking about when we talk about *testing*? At a high-level the philosophy of testing in Finatra revolves around the following testing definitions:

- Feature Tests, the most powerful test enabled by Finatra, allow you to verify all feature requirements of the service by exercising the external interface. Finatra supports both “blackbox testing” and “whitebox testing” against a locally running version of your server. You can selectively swap out certain classes, insert mocks, and perform assertions on internal state.
- Integration Tests, similar to feature tests, but the entire service is not started. Instead, a list of “modules” are loaded, and then method calls and assertions are performed at the class-level.
- Unit Tests, these are method-level tests of a single class, and the framework stays out of your way.
- System Tests -- we also have a concept of larger system testing at a few levels. After release we run a set of blackbox tests as a client of the system, exercising as much of the code as possible to verify functionality post-release. Additionally, we run continuous data-quality testing every day alerting on inconsistencies.

## <a name="override-modules" href="#override-modules">Override Modules</a>
===============================

For basic information on Modules in Finatra, see [Modules](/finatra/user-guide/getting-started#modules).

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed nec viverra purus, in tristique sapien. Duis eu molestie dolor. Nunc id lectus ac dolor posuere laoreet a at tortor. Ut elementum mi quam, varius consectetur eros suscipit in. Suspendisse ultricies dapibus ex feugiat consectetur. Aliquam massa sapien, egestas eleifend dui ac, scelerisque rhoncus urna. Quisque et magna orci. Etiam nisi augue, sollicitudin sed maximus a, hendrerit non enim. Sed a elementum sem. Fusce suscipit dignissim tincidunt.

Donec hendrerit lorem at hendrerit posuere. Suspendisse metus lacus, molestie ac lobortis vitae, lacinia eu eros. Morbi sodales dui id erat luctus placerat. Quisque condimentum lacinia dignissim. Donec congue lacus eu viverra imperdiet. Pellentesque id semper elit. Integer vulputate ipsum a ligula fringilla, vel blandit nisl lacinia. Sed a ultricies magna. Sed massa mauris, tincidunt non iaculis et, pulvinar at urna. Vivamus placerat, mauris luctus lobortis malesuada, urna neque semper sem, a accumsan risus leo at tellus. Nulla et lobortis lectus, non vulputate nibh.

## <a name="test-helpers" href="#test-helpers">Test Helper Classes</a>

![Finatra Test classes](/finatra/images/FinatraTesting.png)

The Finatra testing framework uses the [ScalaTest](http://www.scalatest.org/) testing framework and to facilitate the types of testing outlined above we have several testing traits to aid in creating simple and powerful tests.

### [Feature Tests](http://blog.mattwynne.net/2010/10/22/features-user-stories/)

see: [app/FeatureTest](https://github.com/twitter/finatra/blob/master/inject/inject-app/src/test/scala/com/twitter/inject/app/FeatureTest.scala) and [server/FeatureTest](https://github.com/twitter/finatra/blob/master/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala)

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed nec viverra purus, in tristique sapien. Duis eu molestie dolor. Nunc id lectus ac dolor posuere laoreet a at tortor. Ut elementum mi quam, varius consectetur eros suscipit in. Suspendisse ultricies dapibus ex feugiat consectetur. Aliquam massa sapien, egestas eleifend dui ac, scelerisque rhoncus urna. Quisque et magna orci. Etiam nisi augue, sollicitudin sed maximus a, hendrerit non enim. Sed a elementum sem. Fusce suscipit dignissim tincidunt.

### Integration Tests

see: TestInjector

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed nec viverra purus, in tristique sapien. Duis eu molestie dolor. Nunc id lectus ac dolor posuere laoreet a at tortor. Ut elementum mi quam, varius consectetur eros suscipit in. Suspendisse ultricies dapibus ex feugiat consectetur. Aliquam massa sapien, egestas eleifend dui ac, scelerisque rhoncus urna. Quisque et magna orci. Etiam nisi augue, sollicitudin sed maximus a, hendrerit non enim. Sed a elementum sem. Fusce suscipit dignissim tincidunt.

### Http Tests

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed nec viverra purus, in tristique sapien. Duis eu molestie dolor. Nunc id lectus ac dolor posuere laoreet a at tortor. Ut elementum mi quam, varius consectetur eros suscipit in. Suspendisse ultricies dapibus ex feugiat consectetur. Aliquam massa sapien, egestas eleifend dui ac, scelerisque rhoncus urna. Quisque et magna orci. Etiam nisi augue, sollicitudin sed maximus a, hendrerit non enim. Sed a elementum sem. Fusce suscipit dignissim tincidunt.

### `com.twitter.inject.Mockito`

Provides [Specs2](https://etorreborre.github.io/specs2/) Mockito syntax sugar for [ScalaTest](http://www.scalatest.org/).

This is a drop-in replacement for org.specs2.mock.Mockito. We encourage you to not use `org.specs2.mock.Mockito` directly. Otherwise, match failures won't be  propagated up as ScalaTest test failures.

## Startup Tests

We recommend creating a simple test to check that your service can start up and report itself as healthy. This checks the correctness of the Guice dependency graph, catching errors that can otherwise cause the service to fail to start.

* Startup tests should mimic production as close as possible. As such:
    - avoid using `@Bind` and "override modules" in startup tests.
    - set the Guice `stage` to `PRODUCTION` so that all singletons will be eagerly created at startup (integration/feature tests run in `State.DEVELOPMENT` by default).
    - prevent Finagle clients from making outbound connections during startup tests by setting `com.twitter.server.resolverMap` entries to `nil!`.

For example:

```scala
import com.google.inject.Stage
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class MyServiceStartupTests extends FeatureTest {
  val server = EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new SampleApiServer,
    clientFlags = Map(
      "com.twitter.server.resolverMap" -> "some-thrift-service=nil!"
    ))

  "SampleApiServer" should {
    "startup" in {
      server.assertHealthy()
    }
  }
}
```

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/logging"><span aria-hidden="true">&larr;</span>&nbsp;Logging</a></li>
    <li></li>
  </ul>
</nav>
