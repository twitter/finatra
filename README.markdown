# Finatra [![Build Status](https://secure.travis-ci.org/twitter/finatra.png?branch=master)](http://travis-ci.org/twitter/finatra) [![Coverage Status](https://coveralls.io/repos/twitter/finatra/badge.png?branch=master)](https://coveralls.io/r/twitter/finatra?branch=master)

[Finatra](http://finatra.info) is a sinatra-inspired web framework for scala, running on top of [Finagle](http://twitter.github.com/finagle/)

See the [Getting Started](http://finatra.info/docs/tutorial.html) guide or the [Documentation](http://finatra.info/docs/index.html) for more information.

Get help on the [finatra-users](https://groups.google.com/forum/#!forum/finatra-users) mailing list.

```scala
class HelloWorld extends Controller {

  get("/hello/:name") { request =>
    val name = request.routeParams.get("name").getOrElse("default user")
    render.plain("hello " + name).toFuture
  }

}

object App extends FinatraServer {
  register(new HelloWorld())
}
```

### SBT (dual published for 2.10.x or 2.11.x)

First you need to add the following repository to your build.sbt

```scala
resolvers +=
  "Twitter" at "http://maven.twttr.com"
```

```scala
"com.twitter" %% "finatra" % "1.5.4"
```

### Maven

```xml
<dependency>
  <groupId>com.twitter</groupId>
  <artifactId>finatra_2.10</artifactId>
  <!-- for 2.9 <artifactId>finatra_2.9.2</artifactId> -->
  <version>1.5.3</version>
</dependency>
```

## License

Copyright 2014 Twitter, Inc and other contributors

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
