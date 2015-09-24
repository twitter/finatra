---
layout: page
title: "Testing"
comments: false
sharing: false
footer: true
---

Testing is a core motivation to the Finatra framework and Finatra provides a powerful testing framework allowing for very robust tests of different types.

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

### Feature Tests

See: http://blog.mattwynne.net/2010/10/22/features-user-stories/

see: [app/FeatureTest](https://github.com/twitter/finatra/blob/master/inject/inject-app/src/test/scala/com/twitter/inject/app/FeatureTest.scala) and [server/FeatureTest](https://github.com/twitter/finatra/blob/master/inject/inject-server/src/test/scala/com/twitter/inject/server/FeatureTest.scala)

### Integration Tests

see: TestInjector


## <a name="override-modules" href="#override-modules">Override Modules</a>
===============================
