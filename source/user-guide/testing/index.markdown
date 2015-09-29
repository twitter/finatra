---
layout: page
title: "Testing"
comments: false
sharing: false
footer: true
---

Ease of testing is a core philosophy of the Finatra framework and Finatra provides a powerful testing framework allowing for very robust tests of different types.

## Basics

Finatra provides the following testing features:

- the ability to start a locally running service and easily issue requests against it and assertion behavior on the responses.
- the ability to easily replace or override class implementations throughout the object graph.
- the ability to retrieve classes in the object graph to perform assertions on them.
- the ability to write tests and deploy test instances without deploying test code to production.


## Types of testing

- Feature Tests, the most powerful test enabled by Finatra, allow you to verify all feature requirements of the service by exercising the external interface. Finatra supports both “blackbox testing” and “whitebox testing” against a locally running version of your server. You can selectively swap out certain classes, insert mocks, and perform assertions on internal state.
- Integration Tests, similar to feature tests, but the entire service is not started. Instead, a list of “modules” are loaded, and then method calls and assertions are performed at the class-level.
- Unit Tests, these are method-level tests of a single class, and the framework stays out of your way.
- System Tests -- we also have a concept of larger system testing at a few levels. After release we run a set of blackbox tests as a client of the system, exercising as much of the code as possible to verify functionality post-release. Additionally, we run continuous data-quality testing every day alerting on inconsistencies.

### [Feature Tests](http://blog.mattwynne.net/2010/10/22/features-user-stories/)

see: [app/FeatureTest](https://github.com/twitter/finatra/blob/master/inject/inject-app/src/test/scala/com/twitter/inject/app/FeatureTest.scala) and [server/FeatureTest](https://github.com/twitter/finatra/blob/master/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala)

### Integration Tests

see: TestInjector

## <a name="override-modules" href="#override-modules">Override Modules</a>
===============================

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed nec viverra purus, in tristique sapien. Duis eu molestie dolor. Nunc id lectus ac dolor posuere laoreet a at tortor. Ut elementum mi quam, varius consectetur eros suscipit in. Suspendisse ultricies dapibus ex feugiat consectetur. Aliquam massa sapien, egestas eleifend dui ac, scelerisque rhoncus urna. Quisque et magna orci. Etiam nisi augue, sollicitudin sed maximus a, hendrerit non enim. Sed a elementum sem. Fusce suscipit dignissim tincidunt.

Donec hendrerit lorem at hendrerit posuere. Suspendisse metus lacus, molestie ac lobortis vitae, lacinia eu eros. Morbi sodales dui id erat luctus placerat. Quisque condimentum lacinia dignissim. Donec congue lacus eu viverra imperdiet. Pellentesque id semper elit. Integer vulputate ipsum a ligula fringilla, vel blandit nisl lacinia. Sed a ultricies magna. Sed massa mauris, tincidunt non iaculis et, pulvinar at urna. Vivamus placerat, mauris luctus lobortis malesuada, urna neque semper sem, a accumsan risus leo at tellus. Nulla et lobortis lectus, non vulputate nibh.

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/logging"><span aria-hidden="true">&larr;</span>&nbsp;Logging</a></li>
    <li></li>
  </ul>
</nav>
