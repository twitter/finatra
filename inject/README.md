Inject
==========================================================
Inject provides libraries for integrating [`twitter-server`][twitter-server] and [`util-app`][util-app] with [Google Guice][guice].

## Libraries

* inject-core - provides scala-support for Google Guice [modules][module], support for [`util-app`][util-app] [`Flags`][flag], and `twitter-server` lifecycle integration.

* inject-app - provides a specialized `App` trait extended from [`util-app`][util-app] [`com.twitter.app.App`](https://github.com/twitter/util/blob/master/util-app/src/main/scala/com/twitter/app/App.scala) along with an [`EmbeddedApp`][embedded-app] for testing.

* inject-server - provides a specialized `TwitterServer` trait extending from [`com.twitter.server.TwitterServer`](https://github.com/twitter/twitter-server/blob/master/src/main/scala/com/twitter/server/TwitterServer.scala) along with an [`EmbeddedTwitterServer`][embedded-twitter-server] for testing.

* inject-modules - provides common Twitter modules.

* inject-thrift-client - support for creating [Finagle][finagle] clients to [Thrift][apache-thrift] services.

* inject-request-scope - provides utilities for working with Guice request [scopes][guice-scopes].

* inject-utils - provides common utilities.


[twitter-server]: https://github.com/twitter/twitter-server
[embedded-app]: https://github.com/twitter/finatra/blob/master/inject/inject-app/src/test/scala/com/twitter/inject/app/EmbeddedApp.scala
[embedded-twitter-server]: https://github.com/twitter/finatra/blob/master/inject/inject-server/src/test/scala/com/twitter/inject/server/EmbeddedTwitterServer.scala
[module]: http://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/Module.html
[finagle]: https://github.com/twitter/finagle
[util-app]: https://github.com/twitter/util/tree/master/util-app
[guice]: https://github.com/google/guice
[flag]: https://github.com/twitter/util/blob/master/util-app/src/main/scala/com/twitter/app/Flag.scala
[apache-thrift]: https://thrift.apache.org/
[guice-scopes]: https://github.com/google/guice/wiki/Scopes


Note:
-----------------------------------------------------------
Classes/objects in internal packages, e.g. `com.twitter.inject.app.internal.*` are Finatra framework internal implementation details.
These are meant to be private to the framework and not intended as publicly accessible as they are details specific to the framework and
are thus more subject to breaking changes. You should not depend on their implementations remaining constant since they are not intended
for use outside of the framework itself.

