---
layout: page
title: "Finatra User Guide"
comments: false
sharing: false
footer: true
---

### Topics:

- [Getting Started](/finatra/user-guide/getting-started)
  - [Modules](/finatra/user-guide/getting-started#modules)
  - [Flags](/finatra/user-guide/getting-started#flags)
  - [Futures](/finatra/user-guide/getting-started#futures)
- [Building a new HTTP Server](/finatra/user-guide/build-new-http-server)
  - [Create a Server Definition](/finatra/user-guide/build-new-http-server#server-definition)
  - [Override Default Behavior](/finatra/user-guide/build-new-http-server#override-defaults)
  - [Add a Controller](/finatra/user-guide/build-new-http-server/controller.html)
    - [Routing](/finatra/user-guide/build-new-http-server/controller.html#controllers-and-routing)
    - [Requests](/finatra/user-guide/build-new-http-server/controller.html#requests)
    - [Responses](/finatra/user-guide/build-new-http-server/controller.html#responses)
  - [Add Filters](/finatra/user-guide/build-new-http-server/filter.html)
    - [Request Scope](/finatra/user-guide/build-new-http-server/filter.html#request-scope)
      - [Using `com.twitter.finagle.http.Request#ctx`](/finatra/user-guide/build-new-http-server/filter.html#request-ctx)
  - [Add an ExceptionMapper](/finatra/user-guide/build-new-http-server/exceptions.html)
  - [Implement a Server "Warmup" Handler](/finatra/user-guide/build-new-http-server/warmup.html)
  - [More information](/finatra/user-guide/build-new-http-server/warmup.html#more-information)
- Building a new Thrift Server - *\*detailed-documentation coming soon\**
- [Working with JSON](/finatra/user-guide/json)
  - [Configuration](/finatra/user-guide/json#configuration)
  - [Customization](/finatra/user-guide/json#jackson-customization)
  - [Integration with Routing](/finatra/user-guide/json#routing-json)
  - [Validation Framework](/finatra/user-guide/json#validation-framework)
- [Working with Files](/finatra/user-guide/files)
  - [File Server](/finatra/user-guide/files#file-server)
  - [Mustache Templating](/finatra/user-guide/files#mustache)
- [Logging](/finatra/user-guide/logging)
  - [Basics](/finatra/user-guide/logging#basics)
  - [Logback](/finatra/user-guide/logging#logback)
  - [Mapped Diagnostic Context Filter](/finatra/user-guide/logging#mdc)
- [Testing](/finatra/user-guide/testing)
  - [Types of Tests](/finatra/user-guide/testing#testing-types)
  - [Embedded Servers/Apps](/finatra/user-guide/testing#embedded-server)
  - [Override Modules](/finatra/user-guide/testing#override-modules)
  - [Using `@Bind`](/finatra/user-guide/testing#at-bind)
  - [Test Helper Classes](/finatra/user-guide/testing#test-helpers)
    - [Feature Tests](/finatra/user-guide/testing#feature-tests)
    - [Integration Tests](/finatra/user-guide/testing#integration-tests)
    - [Http Tests](/finatra/user-guide/testing#http-tests)
  - [Startup Tests](/finatra/user-guide/testing#startup-tests)
- [V1 Migration FAQ](/finatra/user-guide/v1-migration)


### Getting Involved

Finatra is an open source project that welcomes contributions from the greater community. We’re thankful for the many people who have already contributed and if you’re interested, please read the [contributing](https://github.com/twitter/finatra/blob/master/CONTRIBUTING.md) guidelines.

For support feel free to follow and/or tweet at the [@finatra](https://twitter.com/finatra) Twitter account, post questions to the [Gitter](https://gitter.im/) [chat room](https://gitter.im/twitter/finatra), or email the [finatra-users](https://groups.google.com/forum/#!forum/finatra-users) Google group: [finatra-users@googlegroups.com](mailto:finatra-users@googlegroups.com).
