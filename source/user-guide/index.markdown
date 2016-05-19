---
layout: page
title: "Finatra User Guide"
comments: false
sharing: false
footer: true
---

Finatra is uses [Finagle](http://twitter.github.io/finagle/guide/) and [TwitterServer](http://twitter.github.io/twitter-server/) and it is *highly recommended* that you familiarize yourself with those frameworks before getting started.

### Topics:

- [TwitterServer Basics](/finatra/user-guide/twitter-server-basics)
- [Getting Started](/finatra/user-guide/getting-started)
  - [Server Lifecycle](/finatra/user-guide/getting-started#lifecycle)
  - [Modules](/finatra/user-guide/getting-started#modules)
  - [Binding Annotations](/finatra/user-guide/getting-started#binding-annotations)
  - [Flags](/finatra/user-guide/getting-started#flags)
  - [Futures](/finatra/user-guide/getting-started#futures)
- [Logging](/finatra/user-guide/logging)
  - [Basics](/finatra/user-guide/logging#basics)
  - [Logback](/finatra/user-guide/logging#logback)
  - [Mapped Diagnostic Context Filter](/finatra/user-guide/logging#mdc)
- [Building a new HTTP Server](/finatra/user-guide/build-new-http-server)
  - [Create a Server Definition](/finatra/user-guide/build-new-http-server#server-definition)
  - [Override Default Behavior](/finatra/user-guide/build-new-http-server#override-defaults)
  - [Add an HTTP Controller](/finatra/user-guide/build-new-http-server/controller.html)
    - [Routing](/finatra/user-guide/build-new-http-server/controller.html#controllers-and-routing)
      - [Adding a Route to the TwitterServer Admin Interface UI Index](/finatra/user-guide/build-new-http-server/controller.html#admin-paths)
    - [Requests](/finatra/user-guide/build-new-http-server/controller.html#requests)
    - [Responses](/finatra/user-guide/build-new-http-server/controller.html#responses)
  - [Add Filters](/finatra/user-guide/build-new-http-server/filter.html)
    - [Request Scope](/finatra/user-guide/build-new-http-server/filter.html#request-scope)
      - [Using `com.twitter.finagle.http.Request#ctx`](/finatra/user-guide/build-new-http-server/filter.html#request-ctx)
  - [Add an ExceptionMapper](/finatra/user-guide/build-new-http-server/exceptions.html)
  - [Implement a Server "Warmup" Handler](/finatra/user-guide/build-new-http-server/warmup.html)
  - [More information](/finatra/user-guide/build-new-http-server/warmup.html#more-information)
- [Working with JSON](/finatra/user-guide/json)
  - [Configuration](/finatra/user-guide/json#configuration)
  - [Customization](/finatra/user-guide/json#jackson-customization)
  - [Integration with Routing](/finatra/user-guide/json#routing-json)
  - [Validation Framework](/finatra/user-guide/json#validation-framework)
- [Working with Files](/finatra/user-guide/files)
  - [File Server](/finatra/user-guide/files#file-server)
  - [Mustache Templating](/finatra/user-guide/files#mustache)
- [Building a new Thrift Server](/finatra/user-guide/build-new-thrift-server)
  - [Thrift Basics](/finatra/user-guide/build-new-thrift-server#thrift-basics)
  - [Create a Server Definition](/finatra/user-guide/build-new-thrift-server#server-definition)
  - [Add a Thrift Controller](/finatra/user-guide/build-new-thrift-server/controller.html)
  - [Add Filters](/finatra/user-guide/build-new-thrift-server/filter.html)
  - [Implement a Server "Warmup" Handler](/finatra/user-guide/build-new-thrift-server/warmup.html)
  - [More information](/finatra/user-guide/build-new-thrift-server/warmup.html#more-information)
- [Testing](/finatra/user-guide/testing)
  - [Types of Tests](/finatra/user-guide/testing#testing-types)
  - [Embedded Servers and Apps](/finatra/user-guide/testing#embedded-server)
  - [Test Helper Classes](/finatra/user-guide/testing#test-helpers)
    - [Feature Tests](/finatra/user-guide/testing#feature-tests)
    - [Integration Tests](/finatra/user-guide/testing#integration-tests)
    - [Http Tests](/finatra/user-guide/testing#http-tests)
    - [Thrift Tests](/finatra/user-guide/testing#thrift-tests)
  - [Startup Tests](/finatra/user-guide/testing#startup-tests)
  - [Working with Mocks](/finatra/user-guide/testing#mocks)
  - [Override Modules](/finatra/user-guide/testing#override-modules)
  - [Using `@Bind`](/finatra/user-guide/testing#at-bind)
- [V1 Migration FAQ](/finatra/user-guide/v1-migration)

### Getting Involved

Finatra is an open source project that welcomes contributions from the greater community. We’re thankful for the many people who have already contributed and if you’re interested, please read the [contributing](https://github.com/twitter/finatra/blob/develop/CONTRIBUTING.md) guidelines.

For support feel free to follow and/or tweet at the [@finatra](https://twitter.com/finatra) Twitter account, post questions to the [Gitter](https://gitter.im/) [chat room](https://gitter.im/twitter/finatra), or email the [finatra-users](https://groups.google.com/forum/#!forum/finatra-users) Google group: [finatra-users@googlegroups.com](mailto:finatra-users@googlegroups.com).
