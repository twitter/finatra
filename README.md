# Finatra

[![Build Status](https://secure.travis-ci.org/twitter/finatra.png?branch=develop)](http://travis-ci.org/twitter/finatra?branch=develop)
[![Test Coverage](http://codecov.io/github/twitter/finatra/coverage.svg?branch=develop)](http://codecov.io/github/twitter/finatra?branch=develop)
[![Project status](https://img.shields.io/badge/status-active-brightgreen.svg)](#status)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.twitter/finatra-http_2.11/badge.svg)][maven-central]
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/twitter/finatra)

## Status

This project is used in production at Twitter (and many other organizations),
and is being actively developed and maintained.

<img src="./finatra_logo_text.png" title="Finatra Logo" alt="Finatra Logo" height=394 width=679/>

Finatra is a lightweight framework for building fast, testable, scala applications on top of [TwitterServer][twitter-server] and [Finagle][finagle]. Finatra provides an easy-to-use API for creating and [testing](http://twitter.github.io/finatra/user-guide/testing/) [Finagle servers](http://twitter.github.io/finagle/guide/Servers.html) and [apps](http://twitter.github.io/util/docs/#com.twitter.app.App) as well as powerful JSON support, modern logging via [SLF4J][slf4j], [Finagle client](http://twitter.github.io/finagle/guide/Clients.html) utilities, and more.


## Getting involved

* Website: [https://twitter.github.io/finatra/](https://twitter.github.io/finatra/)
* Latest news: [Blog](http://twitter.github.io/finatra/blog/archives/)
* Github Source: [https://github.com/twitter/finatra/](https://github.com/twitter/finatra/)
* Gitter: [https://gitter.im/twitter/finatra](https://gitter.im/twitter/finatra)
* Mailing List: [finatra-users@googlegroups.com](https://groups.google.com/forum/#!forum/finatra-users)


## Features

* Production use [@Twitter](https://twitter.com/).
* ~50 times faster than v1.6 in several benchmarks.
* Powerful Feature and Integration test support.
* Optional JSR-330 Dependency Injection using [Google Guice][guice].
* [Jackson][jackson]-based JSON parsing supporting required fields, default values, and [validations](http://twitter.github.io/finatra/user-guide/json/#validation-framework).
* [Logback][logback] [MDC][mdc] integration with [com.twitter.util.Local][local] for contextual logging across [futures](http://twitter.github.io/util/guide/util-cookbook/futures.html).

## Documentation

To get started, see the [Getting Started](http://twitter.github.io/finatra/user-guide/getting-started) section of our [User Guide](http://twitter.github.io/finatra/user-guide/) to get up and running. Or check out the sections for building [HTTP](http://twitter.github.io/finatra/user-guide/build-new-http-server/) or [Thrift](http://twitter.github.io/finatra/user-guide/build-new-thrift-server/) servers.

## Examples

An HTTP controller and server:

```scala
import com.twitter.finatra.http._

@Singleton
class ExampleController extends Controller {
  get("/") { request: Request =>
    "<h1>Hello, world!</h1>"
  }
}
```

```scala
import com.twitter.finatra.http._

class ExampleServer extends HttpServer {
  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .add[ExampleController]
  }
}
```

A Thrift controller and server:

```scala
import com.twitter.finatra.thrift._

@Singleton
class ExampleThriftController 
  extends Controller
  with MyThriftService.BaseServiceIface {
  
  override val myFunction = handle(MyFunction) { args: MyFunction.Args =>
    ...
  }
}
```

```scala
import com.twitter.finatra.thrift._

class ExampleServer extends ThriftServer {
  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .add[ExampleThriftController]
  }
}
```

## Example Projects

Finatra includes working examples which highlight various features of the framework and include tests. In the [develop branch](https://github.com/twitter/finatra/tree/develop/examples) these examples are included in the root [sbt](http://www.scala-sbt.org/) build and are thus buildable as part of the entire project. In the [master branch](https://github.com/twitter/finatra/tree/master/examples) (or a [release branch](https://github.com/twitter/finatra/tree/finatra-2.7.0/examples)) these examples can be built using their invididual [sbt](http://www.scala-sbt.org/) (or [Maven](http://maven.apache.org/)) build files.

Please take a look through the [examples](/examples) for more detailed information on features, testing, and building with sbt (or Maven).

## Latest version

The [master branch](https://github.com/twitter/finatra/tree/master) in Github tracks the latest stable [release](https://github.com/twitter/finatra/releases), which is currently:

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.twitter/finatra-http_2.11/badge.svg)][maven-central]

available on Maven Central. See the [basics](http://twitter.github.io/finatra/user-guide/getting-started/#dependencies) section in the [User Guide][user-guide] for how to add dependencies.

Releases are done on an approximately monthly schedule. While
[semver](http://semver.org/) is not followed, the
[changelogs](CHANGELOG.md) are detailed and include sections on public API
breaks and changes in runtime behavior.

## Development version

The [develop branch](https://github.com/twitter/finatra/tree/develop) in Github tracks the latest code which is updated every week. If you want to contribute a patch or fix, please use this branch as the basis of your [Pull Request](https://help.github.com/articles/creating-a-pull-request/). 

For more information on providing contributions, please see our [CONTRIBUTING.md](/CONTRIBUTING.md) documentation.

## Presentations

Check out our list of presentations: [Finatra Presentations](http://twitter.github.io/finatra/presentations/).

## Authors

* Steve Cosenza <https://github.com/scosenza>
* Christopher Coco <https://github.com/cacoco>

A full list of [contributors](https://github.com/twitter/finatra/graphs/contributors?type=a) can be found on GitHub.

Follow [@finatra](http://twitter.com/finatra) on Twitter for updates.

## License

Copyright 2013-2017 Twitter, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0

[twitter-server]: https://github.com/twitter/twitter-server
[finagle]: https://github.com/twitter/finagle
[util-app]: https://github.com/twitter/util/tree/master/util-app
[guice]: https://github.com/google/guice
[jackson]: https://github.com/FasterXML/jackson
[logback]: http://logback.qos.ch/
[slf4j]: http://www.slf4j.org/manual.html
[local]: https://github.com/twitter/util/blob/master/util-core/src/main/scala/com/twitter/util/Local.scala
[mdc]: http://logback.qos.ch/manual/mdc.html
[maven-central]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.twitter%22%20AND%20%28a%3A%22finatra-http_2.11%22%20OR%20a%3A%22finatra-thrift_2.11%22%29
[user-guide]: http://twitter.github.io/finatra/user-guide/
